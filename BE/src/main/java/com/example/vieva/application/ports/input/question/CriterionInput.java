package com.example.vieva.application.ports.input.question;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CriterionInput(
        String name,
        String description,
        BigDecimal maxScore,
        List<PerformanceLevelInput> levels,
        Integer orderIndex
) {
}
