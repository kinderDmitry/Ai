package com.jarvis.nextgen;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Validates an LLM-produced plan before any Android action is executed. */
public final class AgentPlanValidator {
    public record Validation(boolean valid, String error, Models.TaskPlan plan) {}
    private AgentPlanValidator() {}

    public static Validation parseAndValidate(String raw, ToolRegistry registry, String original) {
        try {
            String text = raw == null ? "" : raw.trim();
            if (text.startsWith("```")) text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
            JSONObject root = new JSONObject(text);
            String clarification = root.optString("clarification", "").trim();
            JSONArray steps = root.optJSONArray("steps");
            List<Models.PlanStep> result = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            if (steps != null) {
                for (int i=0; i<steps.length(); i++) {
                    JSONObject step = steps.optJSONObject(i);
                    if (step == null) return invalid("Шаг плана имеет неверный формат.");
                    String id = step.optString("id", "step_" + (i+1)).trim();
                    String tool = step.optString("tool", "").trim();
                    if (tool.isBlank() || registry.findByName(tool).isEmpty()) return invalid("План содержит неизвестный инструмент: " + tool);
                    if (!ids.add(id)) return invalid("План содержит повторяющийся идентификатор шага.");
                    String input = extractInput(step);
                    if (input.isBlank()) return invalid("У инструмента " + tool + " отсутствуют параметры.");
                    Tool registered = registry.findByName(tool).orElseThrow();
                    try {
                        JSONObject args = new JSONObject(input);
                        ToolSchemaCatalog.Validation av = ToolSchemaCatalog.validate(registered, args);
                        if (!av.valid()) return invalid(av.error());
                        input = av.executionInput();
                    } catch (org.json.JSONException e) {
                        return invalid("Параметры инструмента " + tool + " должны быть строгим JSON-объектом.");
                    }
                    JSONArray depends = step.optJSONArray("depends_on");
                    List<String> deps = new ArrayList<>();
                    if (depends != null) for (int d=0; d<depends.length(); d++) deps.add(depends.optString(d, "").trim());
                    for (String dep : deps) if (!dep.isBlank() && !ids.contains(dep)) return invalid("Шаг " + id + " ссылается на недоступную зависимость: " + dep);
                    result.add(new Models.PlanStep(id, tool, input, false, deps));
                }
            }
            if (result.isEmpty() && !clarification.isBlank()) result.add(new Models.PlanStep("clarification", "__clarification__", clarification, false, List.of()));
            if (result.isEmpty()) return invalid("AI не создал ни одного действия и не задал уточняющий вопрос.");
            return new Validation(true, "", new Models.TaskPlan(original, result));
        } catch (Exception e) { return invalid("AI вернул некорректный структурированный план."); }
    }

    private static String extractInput(JSONObject step) {
        JSONObject args = step.optJSONObject("arguments");
        if (args != null) return args.toString();
        return step.optString("input", "").trim();
    }
    private static Validation invalid(String e) { return new Validation(false, e, null); }
}
