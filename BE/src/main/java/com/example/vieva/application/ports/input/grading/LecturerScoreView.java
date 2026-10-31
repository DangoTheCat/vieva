package com.example.vieva.application.ports.input.grading;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LecturerScoreView(
        UUID scoreId,
        UUID attemptId,
        UUID examId,
        String examTitle,
        UUID studentId,
        String studentName,
        BigDecimal totalScore,
        BigDecimal overrideTotalScore,
        String grade,
        Instant createdAt
) {}
