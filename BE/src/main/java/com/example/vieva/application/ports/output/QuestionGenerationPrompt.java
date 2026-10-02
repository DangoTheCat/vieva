package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.BloomLevel;

import java.util.List;
import java.util.Map;

/**
 * Everything the LLM needs for one call. Context chunks are referenced by short refs
 * (C1, C2, ...) so the model never has to copy UUIDs.
 *
 * @param previousContent  single-question regeneration: the draft being replaced
 * @param lecturerFeedback single-question regeneration: why it is being replaced
 * @param avoidQuestions   existing questions the model must not repeat
 */
public record QuestionGenerationPrompt(
        String subjectName,
        String topicName,
        Map<BloomLevel, Integer> bloomDistribution,
        String lecturerNote,
        List<ContextChunk> contextChunks,
        List<String> avoidQuestions,
        String previousContent,
        String lecturerFeedback
) {
    public int totalQuestions() {
        return bloomDistribution.values().stream().mapToInt(Integer::intValue).sum();
    }

    public record ContextChunk(String ref, String documentName, Integer page, String content) {
    }
}
