package com.example.vieva.application.ports.input.grading;

import java.math.BigDecimal;

public record OverrideScoreCommand(
        BigDecimal overrideTotalScore,
        String reason
) {}
