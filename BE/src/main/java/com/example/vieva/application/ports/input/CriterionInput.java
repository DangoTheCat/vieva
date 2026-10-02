package com.example.vieva.application.ports.input;

import com.example.vieva.domain.entities.PerformanceLevel;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CriterionInput(
        String name,
        String description,
        BigDecimal maxScore,
        List<PerformanceLevel> levels,
        Integer orderIndex
) {
}
