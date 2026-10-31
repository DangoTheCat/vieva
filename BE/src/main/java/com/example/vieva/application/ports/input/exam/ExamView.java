package com.example.vieva.application.ports.input.exam;


import java.time.Instant;
import java.util.UUID;

public record ExamView(
        UUID id,
        UUID subjectId,
        UUID creatorId,
        String title,
        int durationSeconds,
        String status,
        int questionCount,
        Instant createdAt,
        Instant updatedAt,
        int answerTimeLimitSeconds,
        int maxFollowUpsPerQuestion
) {}
