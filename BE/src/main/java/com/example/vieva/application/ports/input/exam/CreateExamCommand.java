package com.example.vieva.application.ports.input.exam;

import java.util.UUID;

public record CreateExamCommand(
        UUID subjectId,
        String title,
        Integer durationSeconds,
        Integer answerTimeLimitSeconds,
        Integer maxFollowUpsPerQuestion
) {
    public CreateExamCommand(UUID subjectId, String title, Integer durationSeconds) {
        this(subjectId, title, durationSeconds, null, null);
    }
}
