package com.jarvis.nextgen;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;

public class ExecutionRecoveryTest {
    @Test public void dependencyMustBeCompletedBeforeStep() {
        Models.TaskPlan plan = new Models.TaskPlan("x", List.of(
                new Models.PlanStep("one", "timer", "20", false, List.of()),
                new Models.PlanStep("two", "music", "", false, List.of("one"))));
        assertNull(ExecutionRecovery.dependencyError(plan, 1));
    }

    @Test public void missingDependencyIsReported() {
        Models.TaskPlan plan = new Models.TaskPlan("x", List.of(
                new Models.PlanStep("two", "music", "", false, List.of("one"))));
        assertNotNull(ExecutionRecovery.dependencyError(plan, 0));
    }
}
