package com.jarvis.nextgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TaskPlanner {
    private final ToolRegistry registry;
    private static final Pattern SPLIT = Pattern.compile(
            "\\s+(?:и затем|затем|потом|после этого)\\s+|;|,\\s*(?=(?:открой|запусти|поставь|найди|поищи|позвони|напомни|включи|посчитай|отправь|запомни|забудь))",
            Pattern.CASE_INSENSITIVE);

    public TaskPlanner(ToolRegistry registry) { this.registry = registry; }

    public Models.TaskPlan plan(String input) {
        String[] raw = SPLIT.split(input);
        List<Models.PlanStep> steps = new ArrayList<>();
        for (String value : raw) {
            String step = value.trim();
            if (step.isEmpty()) continue;
            Optional<Tool> tool = registry.find(step);
            steps.add(new Models.PlanStep(tool.map(Tool::name).orElse("unknown"), step, false));
        }
        return new Models.TaskPlan(input, steps);
    }
}
