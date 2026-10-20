package com.example.vieva.application.ports.input.question;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record QuestionResult(
        UUID questionId,
        String questionCode,
        UUID subjectId,
        UUID topicId,
        String status,
        UUID currentApprovedVersionId,
        Instant createdAt,
        Instant updatedAt
) {
}
