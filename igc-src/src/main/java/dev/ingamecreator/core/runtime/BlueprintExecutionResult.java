package dev.ingamecreator.core.runtime;

import java.util.List;

public record BlueprintExecutionResult(boolean success, int steps, List<String> diagnostics) {
    public BlueprintExecutionResult {
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    public static BlueprintExecutionResult success(int steps) {
        return new BlueprintExecutionResult(true, steps, List.of());
    }

    public static BlueprintExecutionResult failure(int steps, List<String> diagnostics) {
        return new BlueprintExecutionResult(false, steps, diagnostics);
    }
}
