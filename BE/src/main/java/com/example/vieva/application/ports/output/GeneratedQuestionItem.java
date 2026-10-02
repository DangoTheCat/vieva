package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.BloomLevel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GeneratedQuestionItem(
    String questionContent,
    String referenceAnswer,
    BloomLevel bloomLevel,
    String citationQuote,
    UUID chunkId,
    String documentName,
    double similarityScore,
    String rubricName,
    BigDecimal totalPoints,
    String rubricDescription,
    List<GeneratedCriterionItem> criteria
) {}
