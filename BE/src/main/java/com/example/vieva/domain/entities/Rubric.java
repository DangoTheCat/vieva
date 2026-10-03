package com.example.vieva.domain.entities;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * Khung Rubric đánh giá; quan hệ 1:1 với QuestionVersion.
 * BR-02: total_points = Σ max_points of the criteria, no extra weighting.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rubric {
    public static final String DEFAULT_NAME = "Rubric đánh giá";

    private UUID rubricId;
    private UUID questionVersionId;
    private String rubricName;
    private BigDecimal totalPoints;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public static Rubric create(UUID questionVersionId, String name, String description) {
        Instant now = Instant.now();
        return Rubric.builder()
                .rubricId(UUID.randomUUID())
                .questionVersionId(questionVersionId)
                .rubricName(name == null || name.isBlank() ? DEFAULT_NAME : name.trim())
                .description(description)
                .totalPoints(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static BigDecimal sumOf(Collection<RubricCriterion> criteria) {
        if (criteria == null) {
            return BigDecimal.ZERO;
        }
        return criteria.stream()
                .filter(Objects::nonNull)
                .map(RubricCriterion::getMaxPoints)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Recomputes total_points from the criteria (UC1.7). */
    public void recalculateTotal(Collection<RubricCriterion> criteria) {
        this.totalPoints = sumOf(criteria);
        this.updatedAt = Instant.now();
    }

    public boolean totalMatches(Collection<RubricCriterion> criteria) {
        return totalPoints != null && sumOf(criteria).compareTo(totalPoints) == 0;
    }

    public void rename(String name, String newDescription) {
        if (name != null && !name.isBlank()) {
            this.rubricName = name.trim();
        }
        if (newDescription != null) {
            this.description = newDescription.trim();
        }
        this.updatedAt = Instant.now();
    }

    public Rubric copyTo(UUID newVersionId) {
        Instant now = Instant.now();
        return Rubric.builder()
                .rubricId(UUID.randomUUID())
                .questionVersionId(newVersionId)
                .rubricName(rubricName)
                .totalPoints(totalPoints)
                .description(description)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
