package com.example.vieva.application.ports.input.exam;


import java.util.UUID;

public record StudentExamView(
        UUID id,
        UUID subjectId,
        String title,
        int durationSeconds,
        int questionCount,
        String studentAttemptStatus,
        UUID studentAttemptId
) {}
