package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.GradeLevel;
import com.example.vieva.domain.entities.ScoringMode;

import java.math.BigDecimal;

public record AttemptGradingResult(
        BigDecimal totalScore,
        GradeLevel gradeLevel,
        String detailedFeedback,
        ScoringMode scoringMode
) {
}
