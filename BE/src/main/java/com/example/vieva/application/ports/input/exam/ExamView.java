package com.example.vieva.application.ports.input.exam;

import com.example.vieva.domain.entities.ExamStatus;

import java.time.Instant;
import java.util.UUID;

public record ExamView(
        UUID id,
        UUID subjectId,
        UUID creatorId,
        String title,
        int durationSeconds,
        ExamStatus status,
        int questionCount,
        Instant createdAt,
        Instant updatedAt
) {}
