package com.example.vieva.application.ports.input.exam;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StudentScoreView(
        UUID attemptId,
        UUID examId,
        String examTitle,
        BigDecimal totalScore,
        String gradeLevel,
        Instant completedAt
) {}
