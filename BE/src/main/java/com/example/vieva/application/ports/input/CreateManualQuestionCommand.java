package com.example.vieva.application.ports.input;

import com.example.vieva.domain.entities.BloomLevel;
import lombok.Builder;

import java.util.UUID;

/**
 * UC1.5 input. Topic is optional; no document is required.
 */
@Builder
public record CreateManualQuestionCommand(
        UUID topicId,
        String content,
        String expectedAnswer,
        BloomLevel bloomLevel,
        RubricInput rubric
) {
}
