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
        Instant updatedAt,
        Instant askedAt,
        boolean timedOut
) {
    public ExamTurnDetailView(
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
        this(turnId, attemptId, questionId, questionVersionId, turnType, parentTurnId, turnOrder, questionText,
                ttsAudioUrl, studentAudioUrl, transcriptText, status, score, feedback, rubricEvaluations,
                createdAt, updatedAt, null, false);
    }
}
