package com.example.vieva.application.ports.input.exam;

import com.example.vieva.domain.entities.AttemptStatus;

import java.util.UUID;

public record StudentExamView(
        UUID id,
        UUID subjectId,
        String title,
        int durationSeconds,
        int questionCount,
        AttemptStatus studentAttemptStatus,
        UUID studentAttemptId
) {}
