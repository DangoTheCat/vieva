package com.example.vieva.application.ports.input.exam;

import com.example.vieva.domain.entities.GradeLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StudentScoreView(
        UUID attemptId,
        UUID examId,
        String examTitle,
        BigDecimal totalScore,
        GradeLevel gradeLevel,
        Instant completedAt
) {}
