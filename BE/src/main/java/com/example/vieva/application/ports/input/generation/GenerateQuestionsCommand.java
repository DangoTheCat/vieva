package com.example.vieva.application.ports.input.generation;

import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UC1.2 input: READY documents of the subject, total questions and the number per Bloom level.
 */
@Builder
public record GenerateQuestionsCommand(
        UUID subjectId,
        UUID topicId,
        List<UUID> documentIds,
        int totalQuestions,
        Map<String, Integer> bloomDistribution,
        String lecturerNote
) {
}
