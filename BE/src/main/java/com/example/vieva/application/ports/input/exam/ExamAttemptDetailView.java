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
        Instant pausedAt,
        Integer remainingSeconds,
        Instant graceDeadline,
        String detailedFeedback,
        String scoringMode,
        List<ExamTurnDetailView> turns,
        Integer answerTimeLimitSeconds,
        Integer maxFollowUpsPerQuestion
) {
    public ExamAttemptDetailView(
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
            Instant pausedAt,
            Integer remainingSeconds,
            Instant graceDeadline,
            String detailedFeedback,
            String scoringMode,
            List<ExamTurnDetailView> turns
    ) {
        this(attemptId, studentId, subjectId, status, currentQuestionIndex, totalQuestions, totalScore, gradeLevel,
                startedAt, completedAt, pausedAt, remainingSeconds, graceDeadline, detailedFeedback, scoringMode,
                turns, null, null);
    }

    public ExamAttemptDetailView(
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
        this(attemptId, studentId, subjectId, status, currentQuestionIndex, totalQuestions, totalScore, gradeLevel,
                startedAt, completedAt, null, null, null, detailedFeedback, scoringMode, turns);
    }
}
