package com.example.vieva.domain.entities;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GradeLevelTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "10.00, EXCELLENT", "8.50, EXCELLENT", "8.49, GOOD", "7.00, GOOD",
            "6.99, AVERAGE", "5.00, AVERAGE", "4.99, POOR", "0.00, POOR"
    })
    void bandsFollowTheBusinessRule(String score, GradeLevel expected) {
        assertThat(GradeLevel.fromScore(new BigDecimal(score))).isEqualTo(expected);
    }

    @org.junit.jupiter.api.Test
    void nullScoreIsPoor() {
        assertThat(GradeLevel.fromScore(null)).isEqualTo(GradeLevel.POOR);
    }
}
