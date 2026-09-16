package com.jarvis.nextgen;

import org.json.JSONArray;
import org.json.JSONObject;

/** Central catalog of strict JSON contracts exposed to the planner.
 *  The request field intentionally preserves compatibility with the existing
 *  natural-language tool executors while making the planner contract typed,
 *  closed and validated. Tool-specific fields can be promoted here without
 *  changing the orchestrator.
 */
public final class ToolSchemaCatalog {
    private ToolSchemaCatalog() {}

    public static JSONObject schema(Tool tool) {
        JSONObject arguments = new JSONObject()
                .put("type", "object")
                .put("additionalProperties", false)
                .put("properties", new JSONObject()
                        .put("request", new JSONObject()
                                .put("type", "string")
                                .put("minLength", 1)
                                .put("description", "Validated task parameters in natural language for this registered tool.")))
                .put("required", new JSONArray().put("request"));

        return new JSONObject()
                .put("name", tool.name())
                .put("description", tool.description())
                .put("input", arguments)
                .put("strict", true)
                .put("execution_input", "request");
    }

    public static Validation validate(Tool tool, JSONObject args) {
        if (args == null) return Validation.fail("Параметры инструмента должны быть JSON-объектом.");
        JSONArray names = args.names();
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                String key = names.optString(i, "");
                if (!"request".equals(key)) return Validation.fail("Инструмент " + tool.name() + " не поддерживает параметр: " + key);
            }
        }
        if (!args.has("request") || args.isNull("request")) return Validation.fail("У инструмента " + tool.name() + " отсутствует обязательный параметр request.");
        Object value = args.opt("request");
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) return Validation.fail("Параметр request инструмента " + tool.name() + " должен быть непустой строкой.");
        return Validation.ok(((String) value).trim());
    }

    public record Validation(boolean valid, String error, String executionInput) {
        static Validation ok(String value) { return new Validation(true, "", value); }
        static Validation fail(String error) { return new Validation(false, error, ""); }
    }
}
