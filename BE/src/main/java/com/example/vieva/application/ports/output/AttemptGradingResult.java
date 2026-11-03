package com.example.vieva.application.ports.output;

import java.math.BigDecimal;

public record AttemptGradingResult(
        BigDecimal totalScore,
        String gradeLevel,
        String detailedFeedback,
        String scoringMode
) {
}
