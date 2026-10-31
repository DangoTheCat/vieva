package com.example.vieva.domain.entities;

import java.math.BigDecimal;

/**
 * Grade bands on the 0-10 scale. This is the single home of the banding rule (BR: ≥8.5 EXCELLENT,
 * ≥7.0 GOOD, ≥5.0 AVERAGE, otherwise POOR); every grader derives the grade from the score here.
 */
public enum GradeLevel {
    EXCELLENT,
    GOOD,
    AVERAGE,
    POOR;

    private static final BigDecimal EXCELLENT_MIN = new BigDecimal("8.5");
    private static final BigDecimal GOOD_MIN = new BigDecimal("7.0");
    private static final BigDecimal AVERAGE_MIN = new BigDecimal("5.0");

    public static GradeLevel fromScore(BigDecimal score) {
        if (score == null) {
            return POOR;
        }
        if (score.compareTo(EXCELLENT_MIN) >= 0) {
            return EXCELLENT;
        }
        if (score.compareTo(GOOD_MIN) >= 0) {
            return GOOD;
        }
        if (score.compareTo(AVERAGE_MIN) >= 0) {
            return AVERAGE;
        }
        return POOR;
    }
}
