package com.example.vieva.application.ports.input.admin;

import com.example.vieva.domain.entities.GradeLevel;

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
        GradeLevel gradeLevel,
        Instant completedAt
) {}
