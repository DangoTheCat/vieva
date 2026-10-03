package com.example.vieva.application.ports.input;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param totalScore optional; when given it must equal the sum of criteria max scores (BR-02),
 *                   when omitted the total is computed
 */
@Builder
public record RubricInput(
        String name,
        String description,
        BigDecimal totalScore,
        List<CriterionInput> criteria
) {
}
