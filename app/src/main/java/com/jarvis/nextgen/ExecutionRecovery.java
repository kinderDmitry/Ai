package com.jarvis.nextgen;

import java.util.HashSet;
import java.util.Set;

/** Validates dependency ordering and gives the runner a deterministic recovery decision. */
public final class ExecutionRecovery {
    public static String dependencyError(Models.TaskPlan plan, int index) {
        if (index < 0 || index >= plan.steps().size()) return "Недопустимый шаг плана.";
        Models.PlanStep step = plan.steps().get(index);
        Set<String> known = new HashSet<>();
        for (int i=0;i<index;i++) known.add(plan.steps().get(i).id());
        for (String dep : step.dependsOn()) if (!known.contains(dep)) return "Шаг " + step.id() + " зависит от невыполненного шага " + dep + ".";
        return null;
    }
    private ExecutionRecovery() {}
}
