package com.example.vieva.domain.entities;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Khung Rubric đánh giá; quan hệ 1:1 với QuestionVersion; APPROVED bắt buộc có Rubric hợp lệ.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rubric {
    private UUID rubricId;
    private UUID questionVersionId;
    private String rubricName;
    private BigDecimal totalPoints;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Enforces that the sum of all criteria maxPoints must equal this rubric's totalPoints.
     *
     * @throws IllegalArgumentException when totalPoints is missing or the sum doesn't match
     */
    public void validateTotalPoints(java.util.Collection<RubricCriterion> criteria) {
        if (this.totalPoints == null) {
            throw new IllegalArgumentException("Rubric totalPoints must not be null");
        }
        BigDecimal sum = BigDecimal.ZERO;
        if (criteria != null) {
            for (RubricCriterion criterion : criteria) {
                if (criterion == null || criterion.getMaxPoints() == null) {
                    throw new IllegalArgumentException("Criterion maxPoints must not be null");
                }
                sum = sum.add(criterion.getMaxPoints());
            }
        }
        if (sum.compareTo(this.totalPoints) != 0) {
            throw new IllegalArgumentException(
                    "Sum of criterion maxPoints (" + sum + ") must equal rubric totalPoints (" + this.totalPoints + ")");
        }
    }
}
