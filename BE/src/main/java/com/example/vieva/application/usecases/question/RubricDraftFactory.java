package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.input.CriterionInput;
import com.example.vieva.application.ports.input.RubricInput;
import com.example.vieva.domain.entities.PerformanceLevel;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.domain.exception.FieldViolation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Turns rubric input into a Rubric + criteria (BR-02), collecting every field error instead of
 * failing on the first one. The total is always Σ max scores; a provided total must match it.
 */
public final class RubricDraftFactory {

    private RubricDraftFactory() {
    }

    public record RubricDraft(Rubric rubric, List<RubricCriterion> criteria) {
    }

    public static RubricDraft build(UUID versionId, RubricInput input, List<FieldViolation> violations) {
        if (input == null || input.criteria() == null || input.criteria().isEmpty()) {
            violations.add(FieldViolation.of("rubric.criteria", ErrorCode.RUBRIC_REQUIRED,
                    "Rubric must have at least one criterion"));
            return null;
        }
        Rubric rubric = Rubric.create(versionId, input.name(), input.description());
        List<RubricCriterion> criteria = new ArrayList<>();
        int before = violations.size();
        for (int i = 0; i < input.criteria().size(); i++) {
            RubricCriterion criterion = buildCriterion(rubric.getRubricId(), input.criteria().get(i), i + 1,
                    "rubric.criteria[" + i + "]", violations);
            if (criterion != null) {
                criteria.add(criterion);
            }
        }
        if (violations.size() > before) {
            return null;
        }
        rubric.recalculateTotal(criteria);
        if (input.totalScore() != null && input.totalScore().compareTo(rubric.getTotalPoints()) != 0) {
            violations.add(FieldViolation.of("rubric.totalScore", ErrorCode.RUBRIC_SCORE_MISMATCH,
                    "Total score " + input.totalScore() + " must equal the sum of criteria max scores ("
                            + rubric.getTotalPoints() + ")"));
            return null;
        }
        return new RubricDraft(rubric, criteria);
    }

    /**
     * @param defaultOrder used when the input has no explicit order index
     * @return the criterion, or {@code null} when a violation was recorded
     */
    public static RubricCriterion buildCriterion(UUID rubricId, CriterionInput input, int defaultOrder,
                                                 String field, List<FieldViolation> violations) {
        int before = violations.size();
        if (input == null) {
            violations.add(FieldViolation.of(field, ErrorCode.RUBRIC_REQUIRED, "Criterion is required"));
            return null;
        }
        if (isBlank(input.name())) {
            violations.add(FieldViolation.of(field + ".name", ErrorCode.RUBRIC_REQUIRED, "Criterion name is required"));
        }
        if (isBlank(input.description())) {
            violations.add(FieldViolation.of(field + ".description", ErrorCode.RUBRIC_REQUIRED,
                    "Criterion description (achievement levels) is required"));
        }
        BigDecimal maxScore = input.maxScore();
        if (maxScore == null || maxScore.signum() <= 0) {
            violations.add(FieldViolation.of(field + ".maxScore", ErrorCode.RUBRIC_SCORE_MISMATCH,
                    "Criterion max score must be greater than 0"));
        }
        List<PerformanceLevel> levels = input.levels() == null ? List.of() : input.levels();
        for (int i = 0; i < levels.size(); i++) {
            PerformanceLevel level = levels.get(i);
            String levelField = field + ".levels[" + i + "]";
            if (level == null || isBlank(level.label()) || level.score() == null) {
                violations.add(FieldViolation.of(levelField, ErrorCode.INVALID_REQUEST,
                        "Performance level needs a label and a score"));
            } else if (maxScore != null && (level.score().signum() < 0 || level.score().compareTo(maxScore) > 0)) {
                violations.add(FieldViolation.of(levelField + ".score", ErrorCode.RUBRIC_SCORE_MISMATCH,
                        "Performance level score must be between 0 and the criterion max score"));
            }
        }
        if (violations.size() > before) {
            return null;
        }
        int order = input.orderIndex() != null && input.orderIndex() > 0 ? input.orderIndex() : defaultOrder;
        return RubricCriterion.create(rubricId, input.name(), input.description(), maxScore, levels, order);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
