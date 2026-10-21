package com.example.vieva.application.ports.input.exam;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExamTurnDetailView(
        UUID turnId,
        UUID attemptId,
        UUID questionId,
        UUID questionVersionId,
        String turnType,
        UUID parentTurnId,
        int turnOrder,
        String questionText,
        String ttsAudioUrl,
        String studentAudioUrl,
        String transcriptText,
        String status,
        BigDecimal score,
        String feedback,
        String rubricEvaluations,
        Instant createdAt,
        Instant updatedAt
) {
}
