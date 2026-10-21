package com.example.vieva.application.ports.output;

import java.math.BigDecimal;

public record VivaEvaluationResult(
        boolean isStandardMet,
        boolean shouldFollowUp,
        String followUpQuestionText,
        BigDecimal score,
        String feedback,
        String rubricEvaluations
) {
    public VivaEvaluationResult(boolean isStandardMet, boolean shouldFollowUp, String followUpQuestionText, BigDecimal score, String feedback) {
        this(isStandardMet, shouldFollowUp, followUpQuestionText, score, feedback, null);
    }
}
