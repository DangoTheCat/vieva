package com.example.vieva.application.ports.input.grading;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExamParticipantView(
        UUID attemptId,
        UUID studentId,
        String studentName,
        String studentEmail,
        String studentCode,
        String status,
        BigDecimal totalScore,
        BigDecimal aiScore,
        BigDecimal teacherScore,
        BigDecimal overrideTotalScore,
        Instant startedAt,
        Instant completedAt
) {}
