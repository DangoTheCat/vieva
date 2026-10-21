package com.example.vieva.application.ports.input.question;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record RubricResult(
        UUID rubricId,
        String name,
        String description,
        BigDecimal totalScore,
        List<RubricCriterionResult> criteria
) {
}
