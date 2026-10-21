package com.example.vieva.application.ports.input.exam;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExamAttemptView(
        UUID attemptId,
        UUID studentId,
        UUID subjectId,
        String status,
        int currentQuestionIndex,
        int totalQuestions,
        BigDecimal totalScore,
        String gradeLevel,
        Instant startedAt,
        Instant completedAt,
        UUID currentTurnId,
        String currentQuestionText,
        String currentTtsAudioUrl
) {
}
