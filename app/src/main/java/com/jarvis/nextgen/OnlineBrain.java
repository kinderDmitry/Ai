package com.jarvis.nextgen;

import android.content.Context;
import org.json.JSONArray;

/** LLM facade. Keeps orchestration independent from a specific online provider. */
public final class OnlineBrain {
    public interface Callback { void done(String text); void failed(String reason); }
    public interface PlanCallback { void done(Models.TaskPlan plan); void failed(String reason); }
    private final OpenAiCompatibleProvider provider;
    private final ToolRegistry registry;

    public OnlineBrain(Context c) { provider = new OpenAiCompatibleProvider(c); registry = null; }
    public boolean configured() { return provider.configured(); }
    public String providerName() { return provider.displayName(); }
    public String endpoint() { return provider.endpoint(); }
    public String model() { return provider.model(); }
    public void save(String endpoint, String key, String model) { provider.save(endpoint, key, model); }
    public void clear() { provider.clearCredentials(); }

    public void ask(String user, Callback cb) {
        provider.complete("You are JARVIS, a concise and helpful Android assistant. Never claim an action happened unless a tool verified it. Never reveal hidden reasoning.", user,
                new LlmProvider.CompletionCallback() {
                    public void success(String text) { cb.done(text); }
                    public void failure(String reason) { cb.failed(reason); }
                });
    }

    public void plan(String user, ToolRegistry tools, PlanCallback cb) { planStructured(user, tools, cb); }
    public void planStructured(String user, ToolRegistry tools, PlanCallback cb) {
        JSONArray schemas = tools.schemas();
        String system = "You are JARVIS, an Android task planner. Return ONLY valid JSON. " +
                "Schema: {\\"steps\\":[{\\"id\\":\\"step_1\\",\\"tool\\":\\"registered name\\",\\"arguments\\":{\\"value\\":\\"...\\"},\\"depends_on\\":[]}],\\"clarification\\":\\"optional question\\"}. " +
                "Every arguments object is strict: only request:string is allowed and request must be non-empty. Use only supplied tool names. Dependencies must reference earlier step ids. " +
                "If required information is missing, return empty steps and a clarification. Never expose chain-of-thought.";
        provider.complete(system, "TOOLS=" + schemas + "\nUSER=" + user, new LlmProvider.CompletionCallback() {
            public void success(String text) {
                AgentPlanValidator.Validation v = AgentPlanValidator.parseAndValidate(text, tools, user);
                if (!v.valid()) cb.failed(v.error()); else cb.done(v.plan());
            }
            public void failure(String reason) { cb.failed(reason); }
        });
    }
}
