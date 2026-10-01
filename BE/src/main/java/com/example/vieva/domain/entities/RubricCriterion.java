package com.example.vieva.domain.entities;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * Chi tiết các tiêu chí chấm điểm; tổng max_points của các tiêu chí phải bằng total_points của Rubric.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricCriterion {
    private UUID criterionId;
    private UUID rubricId;
    private String criterionName;
    private BigDecimal maxPoints;
    private String achievementDescriptors;
    private Integer orderIndex;
    private Instant createdAt;

    /**
     * Enforces the rule documented on this class: the sum of all criteria {@code maxPoints}
     * must equal the rubric {@code totalPoints}. Call this before persisting a rubric.
     *
     * @throws IllegalArgumentException when a value is missing or the totals do not match
     */
    public static void validateTotalPoints(BigDecimal rubricTotalPoints, Collection<RubricCriterion> criteria) {
        if (rubricTotalPoints == null) {
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
        if (sum.compareTo(rubricTotalPoints) != 0) {
            throw new IllegalArgumentException(
                    "Sum of criterion maxPoints (" + sum + ") must equal rubric totalPoints (" + rubricTotalPoints + ")");
        }
    }
}
