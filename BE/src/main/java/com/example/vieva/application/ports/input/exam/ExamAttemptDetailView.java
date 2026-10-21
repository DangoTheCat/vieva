package com.example.vieva.application.ports.input.exam;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExamAttemptDetailView(
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
        String detailedFeedback,
        String scoringMode,
        List<ExamTurnDetailView> turns
) {
}
