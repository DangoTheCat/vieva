package com.example.vieva.application.ports.input.question;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record QuestionSourceResult(
        UUID sourceId,
        UUID chunkId,
        UUID documentId,
        String documentName,
        String citationQuote,
        BigDecimal similarityScore,
        Integer order
) {
}
