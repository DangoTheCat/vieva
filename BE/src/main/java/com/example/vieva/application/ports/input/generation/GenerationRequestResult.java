package com.example.vieva.application.ports.input.generation;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record GenerationRequestResult(
        UUID generationRequestId,
        UUID subjectId,
        UUID topicId,
        List<UUID> documentIds,
        List<UUID> retrievedChunkIds,
        Map<String, Integer> bloomDistribution,
        int totalQuestions,
        String lecturerNote,
        String status,
        int generatedCount,
        int rejectedCount,
        int attemptCount,
        List<String> issues,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
