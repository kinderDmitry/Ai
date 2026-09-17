package com.jarvis.nextgen;

import static org.junit.Assert.*;
import org.junit.Test;
import org.json.JSONObject;

public class AgentPlanValidatorTest {
    private ToolRegistry registry() {
        ToolRegistry r = new ToolRegistry();
        r.add(new Tool() {
            public String name() { return "timer"; }
            public String description() { return "Create a timer"; }
            public boolean canHandle(String input) { return input.contains("timer"); }
            public Models.ToolResult execute(String input) { return Models.ToolResult.ok("ok"); }
        });
        r.add(new Tool() {
            public String name() { return "music"; }
            public String description() { return "Open music"; }
            public boolean canHandle(String input) { return input.contains("music"); }
            public Models.ToolResult execute(String input) { return Models.ToolResult.ok("ok"); }
        });
        return r;
    }

    @Test public void acceptsValidPlanAndDependencies() {
        String raw = "{\"steps\":[" +
                "{\"id\":\"step_1\",\"tool\":\"timer\",\"input\":\"20 minutes\",\"depends_on\":[]}," +
                "{\"id\":\"step_2\",\"tool\":\"music\",\"input\":\"open music\",\"depends_on\":[\"step_1\"]}]}";
        AgentPlanValidator.Validation v = AgentPlanValidator.parseAndValidate(raw, registry(), "timer then music");
        assertTrue(v.valid());
        assertEquals(2, v.plan().steps().size());
    }

    @Test public void rejectsUnknownTool() {
        String raw = "{\"steps\":[{\"id\":\"step_1\",\"tool\":\"secretTool\",\"input\":\"x\"}]}";
        AgentPlanValidator.Validation v = AgentPlanValidator.parseAndValidate(raw, registry(), "do it");
        assertFalse(v.valid());
        assertNull(v.plan());
    }

    @Test public void acceptsClarificationInsteadOfAction() {
        String raw = "{\"steps\":[],\"clarification\":\"Какой таймер поставить?\"}";
        AgentPlanValidator.Validation v = AgentPlanValidator.parseAndValidate(raw, registry(), "поставь таймер");
        assertTrue(v.valid());
        assertEquals("__clarification__", v.plan().steps().get(0).tool());
    }
}
