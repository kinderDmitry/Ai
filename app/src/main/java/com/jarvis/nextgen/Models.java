package com.jarvis.nextgen;

import java.util.List;

public final class Models {
    public enum State { IDLE, LISTENING, THINKING, PLANNING, EXECUTING, SPEAKING, SUCCESS, ERROR }
    public enum ResultCode { SUCCESS, FAILED, TIMEOUT, CANCELLED, PERMISSION_REQUIRED, UNSUPPORTED, OFFLINE }

    public record ToolResult(ResultCode code, String message, boolean requiresConfirmation,
                              Runnable confirmedAction, Runnable verification) {
        public boolean success() { return code == ResultCode.SUCCESS; }
        public static ToolResult ok(String message) { return new ToolResult(ResultCode.SUCCESS, message, false, null, null); }
        public static ToolResult confirm(String message, Runnable action) {
            return new ToolResult(ResultCode.SUCCESS, message, true, action, null);
        }
        public static ToolResult fail(ResultCode code, String message) {
            return new ToolResult(code, message, false, null, null);
        }
    }

    public record PlanStep(String id, String tool, String input, boolean confirmationRequired, java.util.List<String> dependsOn) {
        public PlanStep(String tool, String input, boolean confirmationRequired) { this("step", tool, input, confirmationRequired, java.util.List.of()); }
    }
    public record TaskPlan(String original, List<PlanStep> steps) {}
    private Models() {}
}
