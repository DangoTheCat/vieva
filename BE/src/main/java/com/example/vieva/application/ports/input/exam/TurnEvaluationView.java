package com.example.vieva.application.ports.input.exam;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TurnEvaluationView(
        UUID turnId,
        UUID attemptId,
        String turnType,
        int turnOrder,
        String status,
        BigDecimal score,
        String feedback,
        String rubricEvaluations,
        String studentAudioUrl,
        String transcriptText,
        boolean isAttemptFinished,
        UUID nextTurnId,
        String nextQuestionText,
        String nextTurnType,
        Instant updatedAt
) {
}
