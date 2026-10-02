package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tiêu chí chấm điểm: tên, mô tả mức đạt, điểm tối đa và (tuỳ chọn) các mức đạt chi tiết.
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
    @Builder.Default
    private List<PerformanceLevel> performanceLevels = new ArrayList<>();
    private Integer orderIndex;
    private Instant createdAt;

    public static RubricCriterion create(UUID rubricId, String name, String description, BigDecimal maxPoints,
                                         List<PerformanceLevel> levels, int orderIndex) {
        RubricCriterion criterion = RubricCriterion.builder()
                .criterionId(UUID.randomUUID())
                .rubricId(rubricId)
                .orderIndex(orderIndex)
                .createdAt(Instant.now())
                .build();
        criterion.update(name, description, maxPoints, levels);
        return criterion;
    }

    public void update(String name, String description, BigDecimal maxPoints, List<PerformanceLevel> levels) {
        if (name == null || name.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Criterion name is required");
        }
        if (description == null || description.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Criterion description is required");
        }
        if (maxPoints == null || maxPoints.signum() <= 0) {
            throw new AppException(ErrorCode.RUBRIC_SCORE_MISMATCH, "Criterion max score must be greater than 0");
        }
        List<PerformanceLevel> safeLevels = levels == null ? new ArrayList<>() : new ArrayList<>(levels);
        for (PerformanceLevel level : safeLevels) {
            if (level == null || level.label() == null || level.label().isBlank() || level.score() == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Each performance level needs a label and a score");
            }
            if (level.score().signum() < 0 || level.score().compareTo(maxPoints) > 0) {
                throw new AppException(ErrorCode.RUBRIC_SCORE_MISMATCH,
                        "Performance level score must be between 0 and the criterion max score");
            }
        }
        this.criterionName = name.trim();
        this.achievementDescriptors = description.trim();
        this.maxPoints = maxPoints;
        this.performanceLevels = safeLevels;
    }

    public RubricCriterion copyTo(UUID newRubricId) {
        return RubricCriterion.builder()
                .criterionId(UUID.randomUUID())
                .rubricId(newRubricId)
                .criterionName(criterionName)
                .maxPoints(maxPoints)
                .achievementDescriptors(achievementDescriptors)
                .performanceLevels(performanceLevels == null ? new ArrayList<>() : new ArrayList<>(performanceLevels))
                .orderIndex(orderIndex)
                .createdAt(Instant.now())
                .build();
    }
}
