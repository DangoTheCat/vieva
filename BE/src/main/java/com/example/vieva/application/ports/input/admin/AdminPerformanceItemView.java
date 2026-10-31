package com.example.vieva.application.ports.input.admin;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminPerformanceItemView(
        UUID attemptId,
        String studentName,
        String studentEmail,
        String examTitle,
        String subjectName,
        BigDecimal totalScore,
        String gradeLevel,
        Instant completedAt
) {}
