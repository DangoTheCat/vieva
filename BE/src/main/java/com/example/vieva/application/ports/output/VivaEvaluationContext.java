package com.example.vieva.application.ports.output;

public record VivaEvaluationContext(
        String expectedAnswer,
        String rubricJson,
        String ragContext
) {
    public static VivaEvaluationContext empty() {
        return new VivaEvaluationContext(null, null, null);
    }
}
