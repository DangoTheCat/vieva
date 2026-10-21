package com.example.vieva.application.ports.input.question;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record RubricCriterionResult(
        UUID criterionId,
        String name,
        String description,
        BigDecimal maxScore,
        List<PerformanceLevelResult> levels,
        Integer orderIndex
) {
}
