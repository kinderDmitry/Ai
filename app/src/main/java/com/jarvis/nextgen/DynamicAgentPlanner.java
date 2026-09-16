package com.jarvis.nextgen;

import java.util.concurrent.atomic.AtomicBoolean;

/** Async structured planner. No plan reaches execution without schema validation. */
public final class DynamicAgentPlanner {
    public interface Callback { void done(Models.TaskPlan plan); void clarification(String text); void failed(String reason); }
    private final OnlineBrain brain;
    private final ToolRegistry registry;
    public DynamicAgentPlanner(OnlineBrain brain, ToolRegistry registry) { this.brain=brain; this.registry=registry; }
    public void plan(String request, Callback callback) {
        brain.planStructured(request, registry, new OnlineBrain.PlanCallback(){
            @Override public void done(Models.TaskPlan plan) {
                if (plan == null || plan.steps().isEmpty()) { callback.failed("Пустой план."); return; }
                if (plan.steps().size()==1 && "__clarification__".equals(plan.steps().get(0).tool())) callback.clarification(plan.steps().get(0).input());
                else callback.done(plan);
            }
            @Override public void failed(String reason) { callback.failed(reason); }
        });
    }
}
