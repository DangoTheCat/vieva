package com.example.vieva.application.ports.input.grading;

import com.example.vieva.domain.entities.AttemptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExamParticipantView(
        UUID attemptId,
        UUID studentId,
        String studentName,
        String studentEmail,
        String studentCode,
        AttemptStatus status,
        BigDecimal totalScore,
        BigDecimal aiScore,
        BigDecimal teacherScore,
        BigDecimal overrideTotalScore,
        Instant startedAt,
        Instant completedAt
) {}
