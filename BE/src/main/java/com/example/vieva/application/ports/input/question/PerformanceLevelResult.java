package com.example.vieva.application.ports.input.question;

import java.math.BigDecimal;

public record PerformanceLevelResult(
        String label,
        String description,
        BigDecimal score
) {
}
