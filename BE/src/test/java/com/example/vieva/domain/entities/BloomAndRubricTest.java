package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BloomAndRubricTest {

    @Test
    @DisplayName("Bloom has exactly the 6 revised levels")
    void sixLevels() {
        assertThat(BloomLevel.values()).containsExactly(BloomLevel.REMEMBER, BloomLevel.UNDERSTAND, BloomLevel.APPLY,
                BloomLevel.ANALYZE, BloomLevel.EVALUATE, BloomLevel.CREATE);
    }

    @ParameterizedTest
    @CsvSource({
            "REMEMBER, REMEMBER", "apply, APPLY", "Nhớ, REMEMBER", "hiểu, UNDERSTAND", "Vận dụng, APPLY",
            "VAN DUNG, APPLY", "Phân tích, ANALYZE", "đánh giá, EVALUATE", "Sáng  tạo, CREATE", "'  create ', CREATE"
    })
    void parsesCodesAndVietnameseLabels(String raw, BloomLevel expected) {
        assertThat(BloomLevel.fromCodeOrLabel(raw)).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "VAN_DUNG_CAO", "KNOWLEDGE", "Nhận biết"})
    void rejectsUnknownLevels(String raw) {
        assertThat(BloomLevel.fromCodeOrLabel(raw)).isEmpty();
    }

    @Test
    @DisplayName("distribution over 6 levels must add up to the total")
    void distributionMustMatchTotal() {
        Map<BloomLevel, Integer> counts = new EnumMap<>(BloomLevel.class);
        counts.put(BloomLevel.REMEMBER, 1);
        counts.put(BloomLevel.UNDERSTAND, 1);
        counts.put(BloomLevel.APPLY, 2);
        counts.put(BloomLevel.ANALYZE, 1);
        counts.put(BloomLevel.EVALUATE, 1);
        counts.put(BloomLevel.CREATE, 0);
        BloomDistribution distribution = BloomDistribution.of(counts, 6);
        assertThat(distribution.total()).isEqualTo(6);
        assertThat(distribution.asMap()).doesNotContainKey(BloomLevel.CREATE);

        assertThatThrownBy(() -> BloomDistribution.of(counts, 7))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_BLOOM_DISTRIBUTION);
        assertThatThrownBy(() -> BloomDistribution.of(Map.of(BloomLevel.APPLY, -1), -1))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_BLOOM_DISTRIBUTION);
        assertThatThrownBy(() -> BloomDistribution.of(Map.of(BloomLevel.APPLY, 0), 0))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_BLOOM_DISTRIBUTION);
        assertThatThrownBy(() -> BloomDistribution.of(Map.of(), 0))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_BLOOM_DISTRIBUTION);
    }

    @Test
    void remainingAfterProducedQuestions() {
        BloomDistribution distribution = BloomDistribution.of(Map.of(BloomLevel.APPLY, 2, BloomLevel.CREATE, 1), 3);
        BloomDistribution remaining = distribution.remainingAfter(Map.of(BloomLevel.APPLY, 1, BloomLevel.REMEMBER, 4));
        assertThat(remaining.asMap()).containsOnly(Map.entry(BloomLevel.APPLY, 1), Map.entry(BloomLevel.CREATE, 1));
        assertThat(distribution.remainingAfter(Map.of(BloomLevel.APPLY, 2, BloomLevel.CREATE, 1))).isNull();
    }

    @Test
    @DisplayName("BR-02: total = Σ max score, no weighting")
    void rubricTotalIsSumOfCriteria() {
        Rubric rubric = Rubric.create(UUID.randomUUID(), null, null);
        assertThat(rubric.getRubricName()).isEqualTo(Rubric.DEFAULT_NAME);
        List<RubricCriterion> criteria = List.of(
                RubricCriterion.create(rubric.getRubricId(), "A", "mô tả", new BigDecimal("2.5"), List.of(), 1),
                RubricCriterion.create(rubric.getRubricId(), "B", "mô tả", new BigDecimal("7.50"), List.of(), 2));
        rubric.recalculateTotal(criteria);
        assertThat(rubric.getTotalPoints()).isEqualByComparingTo("10");
        assertThat(rubric.totalMatches(criteria)).isTrue();

        rubric.setTotalPoints(new BigDecimal("12"));
        assertThat(rubric.totalMatches(criteria)).isFalse();
        assertThat(rubric.totalMatches(List.of())).isFalse();
    }

    @Test
    void criterionValidatesScoresAndLevels() {
        UUID rubricId = UUID.randomUUID();
        assertThatThrownBy(() -> RubricCriterion.create(rubricId, "A", "d", BigDecimal.ZERO, List.of(), 1))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.RUBRIC_SCORE_MISMATCH);
        assertThatThrownBy(() -> RubricCriterion.create(rubricId, " ", "d", BigDecimal.ONE, List.of(), 1))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThatThrownBy(() -> RubricCriterion.create(rubricId, "A", "d", BigDecimal.ONE,
                List.of(new PerformanceLevel("Giỏi", null, new BigDecimal("2"))), 1))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.RUBRIC_SCORE_MISMATCH);

        RubricCriterion criterion = RubricCriterion.create(rubricId, "A", "d", new BigDecimal("4"),
                List.of(new PerformanceLevel("Đạt", "đủ ý", new BigDecimal("4"))), 1);
        RubricCriterion copy = criterion.copyTo(UUID.randomUUID());
        assertThat(copy.getCriterionId()).isNotEqualTo(criterion.getCriterionId());
        assertThat(copy.getPerformanceLevels()).isEqualTo(criterion.getPerformanceLevels());
    }
}
