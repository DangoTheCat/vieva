package com.example.vieva.application.ports.input.question;

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
        String bloomLevel,
        RubricInput rubric
) {
}
