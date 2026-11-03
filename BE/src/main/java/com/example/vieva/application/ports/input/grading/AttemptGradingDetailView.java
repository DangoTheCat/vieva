package com.example.vieva.application.ports.input.grading;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttemptGradingDetailView(
        UUID attemptId,
        UUID examId,
        String examTitle,
        UUID studentId,
        String studentName,
        String studentEmail,
        String status,
        BigDecimal totalScore,
        BigDecimal aiScore,
        BigDecimal teacherScore,
        BigDecimal overrideTotalScore,
        List<QuestionGradingView> questions,
        /** True once the lecturer finalized the grade; only then can the student see it. */
        boolean scorePublished,
        Instant publishedAt
) {}
