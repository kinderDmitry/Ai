package com.jarvis.nextgen;

import android.content.Context;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Coordinates understanding, planning, tools, confirmation, verification and response. */
public final class JarvisOrchestrator {
    public interface Listener {
        void state(Models.State state);
        void reply(String text);
        void askConfirmation(String text, Runnable yes, Runnable no);
    }

    private final ToolRegistry registry;
    private final TaskPlanner planner;
    private final ContextEngine context;
    private final IntentEngine intents;
    private final ActionCenter actions;
    private final OnlineBrain online;
    private final DynamicAgentPlanner dynamicPlanner;
    private final AgentExecutionStore executionStore;
    private final MemoryStore memory;
    private final Listener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile Pending pending;
    private record Pending(String prompt, long actionId, String input, Models.TaskPlan plan, int nextIndex, Tool tool, Models.ToolResult result) {}

    public JarvisOrchestrator(Context context, Listener listener) {
        Context app = context.getApplicationContext();
        this.listener = listener;
        registry = new AndroidTools(app).registry();
        planner = new TaskPlanner(registry);
        this.context = new ContextEngine(app);
        intents = new IntentEngine();
        actions = new ActionCenter(app);
        online = new OnlineBrain(app);
        dynamicPlanner = new DynamicAgentPlanner(online, registry);
        executionStore = new AgentExecutionStore(app);
        memory = new MemoryStore(app);
    }

    public void resumePending() {
        AgentExecutionStore.Snapshot snap = executionStore.load();
        if (snap == null || !executionStore.hasPending()) return;
        listener.reply("Продолжаю незавершённую задачу.");
        listener.state(Models.State.EXECUTING);
        executor.execute(() -> resumeFromSnapshot(snap));
    }

    public void handle(String raw) {
        String input = intents.normalize(raw);
        if (input.isBlank()) { listener.reply("Я не расслышал запрос."); return; }
        if (handleMemoryCommand(input)) return;
        Pending p = pending;
        if (p != null && intents.isConfirmation(input)) { pending = null; continueAfterConfirmation(p); return; }
        if (p != null && intents.isCancellation(input)) { pending = null; actions.cancel(p.actionId(), p.input()); executionStore.save(String.valueOf(p.actionId()),p.input(),p.plan(),p.nextIndex(),AgentExecutionStore.Status.CANCELLED,"Отменено пользователем"); executionStore.clear(); listener.reply("Отменил."); listener.state(Models.State.IDLE); return; }
        context.observe(input);
        String enriched = context.enrich(input);
        List<MemoryStore.Entry> relevant = memory.search(enriched, 5);
        if (!relevant.isEmpty()) enriched = enriched + "\n[Relevant user memory: " + memoryContext(relevant) + "]";
        listener.state(Models.State.THINKING);
        executor.execute(() -> run(enriched));
    }

    private void run(String input) {
        try {
            listener.state(Models.State.PLANNING);
            Models.TaskPlan plan = planner.plan(input);
            if (plan.steps().isEmpty()) { failOnly("Не удалось построить план действия."); return; }

            boolean unknown = plan.steps().stream().anyMatch(s -> "unknown".equals(s.tool()));
            if (unknown) {
                if (!online.configured()) {
                    failOnly("Для свободных вопросов и динамического планирования нужен настроенный онлайн AI.");
                    return;
                }
                listener.state(Models.State.THINKING);
                dynamicPlanner.plan(input, new DynamicAgentPlanner.Callback() {
                    public void done(Models.TaskPlan agentPlan) { executePlan(input, agentPlan); }
                    public void clarification(String text) { listener.reply(text); listener.state(Models.State.IDLE); }
                    public void failed(String reason) {
                        online.ask(input, new OnlineBrain.Callback() {
                            public void done(String text) { listener.reply(text); listener.state(Models.State.SUCCESS); }
                            public void failed(String r) { failOnly(reason); }
                        });
                    }
                });
                return;
            }
            executePlan(input, plan);
        } catch (Exception e) { failOnly("Не удалось завершить задачу: " + safe(e.getMessage())); }
    }

    private void executePlan(String input, Models.TaskPlan plan) {
        try {
            long id = actions.start(input);
            listener.state(Models.State.EXECUTING);
            executionStore.save(String.valueOf(id), input, plan, 0, AgentExecutionStore.Status.RUNNING, "");
            for (int i=0;i<plan.steps().size();i++) {
                String depError = ExecutionRecovery.dependencyError(plan, i);
                if (depError != null) { actions.finish(id,input,false); executionStore.save(String.valueOf(id),input,plan,i,AgentExecutionStore.Status.FAILED,depError); failOnly(depError); return; }
                Models.PlanStep step=plan.steps().get(i);
                Tool tool=registry.findByName(step.tool()).orElse(null);
                if(tool==null){actions.finish(id,input,false);failOnly("Инструмент недоступен: "+step.tool());return;}
                Models.ToolResult result=tool.execute(step.input());
                if(result.requiresConfirmation()){executionStore.save(String.valueOf(id),input,plan,i,AgentExecutionStore.Status.WAITING_CONFIRMATION,result.message());
                    pending=new Pending(result.message(),id,input,plan,i+1,tool,result);listener.askConfirmation(result.message(),()->handle("да"),()->handle("нет"));return;}
                if(!result.success()){actions.finish(id,input,false); executionStore.save(String.valueOf(id),input,plan,i,AgentExecutionStore.Status.FAILED,result.message()); failOnly(result.message());return;}
                Models.ToolResult verified=tool.verify(result);
                if(!verified.success()){actions.finish(id,input,false);failOnly("Действие не прошло проверку: "+verified.message());return;}
                listener.reply(result.message());
            }
            actions.finish(id,input,true); executionStore.save(String.valueOf(id),input,plan,plan.steps().size(),AgentExecutionStore.Status.COMPLETED,"Готово"); executionStore.clear(); listener.state(Models.State.SUCCESS);
            if(plan.steps().size()>1)listener.reply("Готово. Выполнил "+plan.steps().size()+" действия.");
        } catch(Exception e){failOnly("Не удалось завершить задачу: "+safe(e.getMessage()));}
    }

    private void continueAfterConfirmation(Pending p) {
        executor.execute(() -> {
            try {
                listener.state(Models.State.EXECUTING);
                Models.ToolResult r = p.result();
                if (r.confirmedAction() == null) { actions.finish(p.actionId(), p.input(), false); failOnly("Для этого действия отсутствует исполняемый обработчик."); return; }
                r.confirmedAction().run();
                executionStore.save(String.valueOf(p.actionId()),p.input(),p.plan(),p.nextIndex(),AgentExecutionStore.Status.RUNNING,"Подтверждён шаг "+p.tool().name());
                Models.ToolResult verified = r.verification() == null ? Models.ToolResult.ok("Проверка действия выполнена.") : verifyRunnable(r.verification());
                if (!verified.success()) { actions.finish(p.actionId(), p.input(), false); failOnly("Действие не прошло проверку: " + verified.message()); return; }
                listener.reply("Выполнено: " + p.tool().name());
                for (int i = p.nextIndex(); i < p.plan().steps().size(); i++) {
                    Models.PlanStep step = p.plan().steps().get(i);
                    Tool tool = registry.findByName(step.tool()).orElse(null);
                    if (tool == null) { actions.finish(p.actionId(), p.input(), false); failOnly("Инструмент недоступен для: " + step.input()); return; }
                    Models.ToolResult result = tool.execute(step.input());
                    if (result.requiresConfirmation()) {
                        executionStore.save(String.valueOf(p.actionId()),p.input(),p.plan(),i+1,AgentExecutionStore.Status.WAITING_CONFIRMATION,result.message());
                        pending = new Pending(result.message(), p.actionId(), p.input(), p.plan(), i + 1, tool, result);
                        listener.askConfirmation(result.message(), () -> handle("да"), () -> handle("нет"));
                        return;
                    }
                    if (!result.success()) { actions.finish(p.actionId(), p.input(), false); failOnly(result.message()); return; }
                    Models.ToolResult v = tool.verify(result);
                    if (!v.success()) { actions.finish(p.actionId(), p.input(), false); failOnly("Действие не прошло проверку: " + v.message()); return; }
                    listener.reply(result.message());
                }
                actions.finish(p.actionId(), p.input(), true);
                executionStore.save(String.valueOf(p.actionId()),p.input(),p.plan(),p.plan().steps().size(),AgentExecutionStore.Status.COMPLETED,"Готово");
                executionStore.clear();
                listener.reply("Готово.");
                listener.state(Models.State.SUCCESS);
            } catch (Exception e) { actions.finish(p.actionId(), p.input(), false); failOnly("Действие не выполнено: " + safe(e.getMessage())); }
        });
    }

    private void resumeFromSnapshot(AgentExecutionStore.Snapshot snap) {
        try {
            // Snapshot stores the complete plan; reconstruct the executable plan from the durable JSON.
            org.json.JSONArray a = new org.json.JSONArray(snap.planJson());
            java.util.ArrayList<Models.PlanStep> steps = new java.util.ArrayList<>();
            for (int i=0;i<a.length();i++){org.json.JSONObject o=a.getJSONObject(i);steps.add(new Models.PlanStep(o.optString("id","step-"+i),o.getString("tool"),o.optString("input",""),false,java.util.List.of()));}
            Models.TaskPlan plan = new Models.TaskPlan(snap.original(),steps);
            executePlanFromIndex(snap.original(),plan,snap.nextIndex(),Long.parseLong(snap.taskId()));
        } catch(Exception e){ listener.state(Models.State.ERROR); listener.reply("Не удалось восстановить задачу: "+safe(e.getMessage())); }
    }

    private void executePlanFromIndex(String input, Models.TaskPlan plan, int start, long id) {
        for(int i=start;i<plan.steps().size();i++){
            String dep=ExecutionRecovery.dependencyError(plan,i); if(dep!=null){actions.finish(id,input,false);executionStore.clear();failOnly(dep);return;}
            Models.PlanStep step=plan.steps().get(i); Tool tool=registry.findByName(step.tool()).orElse(null);
            if(tool==null){actions.finish(id,input,false);executionStore.clear();failOnly("Инструмент недоступен: "+step.tool());return;}
            Models.ToolResult r=tool.execute(step.input());
            if(r.requiresConfirmation()){executionStore.save(String.valueOf(id),input,plan,i,AgentExecutionStore.Status.WAITING_CONFIRMATION,r.message());pending=new Pending(r.message(),id,input,plan,i+1,tool,r);listener.askConfirmation(r.message(),()->handle("да"),()->handle("нет"));return;}
            if(!r.success()){actions.finish(id,input,false);executionStore.save(String.valueOf(id),input,plan,i,AgentExecutionStore.Status.FAILED,r.message());failOnly(r.message());return;}
            Models.ToolResult v=tool.verify(r); if(!v.success()){actions.finish(id,input,false);executionStore.save(String.valueOf(id),input,plan,i,AgentExecutionStore.Status.FAILED,v.message());failOnly("Действие не прошло проверку: "+v.message());return;}
            listener.reply(r.message()); executionStore.save(String.valueOf(id),input,plan,i+1,AgentExecutionStore.Status.RUNNING,r.message());
        }
        actions.finish(id,input,true);executionStore.save(String.valueOf(id),input,plan,plan.steps().size(),AgentExecutionStore.Status.COMPLETED,"Готово");executionStore.clear();listener.reply("Готово.");listener.state(Models.State.SUCCESS);
    }

    private Models.ToolResult verifyRunnable(Runnable verification) {
        try { verification.run(); return Models.ToolResult.ok("Проверка выполнена."); }
        catch (Exception e) { return Models.ToolResult.fail(Models.ResultCode.FAILED, safe(e.getMessage())); }
    }


    private boolean handleMemoryCommand(String input) {
        String x=input.trim(); String lower=x.toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("что ты обо мне помнишь") || lower.equals("покажи мою память") || lower.equals("what do you remember about me")) {
            List<MemoryStore.Entry> entries=memory.all();
            if(entries.isEmpty()){listener.reply("Пока ничего не сохранено.");} else {listener.reply(memoryContext(entries));}
            return true;
        }
        if (lower.equals("удали всю память") || lower.equals("удалить всю память") || lower.equals("забудь всё") || lower.equals("forget everything")) {
            memory.clear(); listener.reply("Память очищена."); return true;
        }
        java.util.regex.Matcher forget=java.util.regex.Pattern.compile("(?i)^(?:забудь|удали из памяти|forget)\\s+(.+)$").matcher(x);
        if(forget.find()){String key=forget.group(1).trim();memory.forget(key);listener.reply("Удалил из памяти: "+key);return true;}
        java.util.regex.Matcher remember=java.util.regex.Pattern.compile("(?i)^(?:запомни|запомни что|remember)\\s+(.+?)\\s*(?:=|:|\\bчто\\b)\\s*(.+)$").matcher(x);
        if(remember.find()){String key=remember.group(1).trim();String value=remember.group(2).trim();memory.remember("USER_PREFERENCE",key,value);listener.reply("Запомнил: "+key+" — "+value);return true;}
        return false;
    }

    private String memoryContext(List<MemoryStore.Entry> entries){
        StringBuilder b=new StringBuilder();int n=0;for(MemoryStore.Entry e:entries){if(n++>0)b.append("; ");b.append(e.key()).append(" = ").append(e.value());if(n>=8)break;}return b.toString();
    }

    private void failOnly(String message) { listener.state(Models.State.ERROR); listener.reply(message); }
    private String safe(String s) { return s == null || s.isBlank() ? "неизвестная ошибка" : s; }
    public synchronized AgentExecutionStore.Snapshot pendingExecution() { return executionStore.load(); }
    public void cancelPending() {
        executor.execute(() -> {
            AgentExecutionStore.Snapshot s=executionStore.load();
            if(s==null){listener.reply("Нет незавершённой задачи.");return;}
            executionStore.setStatus(AgentExecutionStore.Status.CANCELLED,"Отменено пользователем");
            try { actions.cancel(Long.parseLong(s.taskId()),s.original()); } catch(Exception ignored) {}
            pending=null; listener.reply("Незавершённую задачу отменил."); listener.state(Models.State.IDLE);
        });
    }
    public List<Tool> tools() { return registry.all(); }
    public void shutdown() { executor.shutdownNow(); }
}
