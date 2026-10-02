package com.example.vieva.domain.services;

import com.example.vieva.domain.entities.BloomLevel;
import com.example.vieva.domain.entities.DocumentChunk;
import com.example.vieva.domain.entities.QuestionGenerationMode;
import com.example.vieva.domain.entities.QuestionSource;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.RubricCriterion;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.domain.exception.FieldViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionApprovalPolicyTest {

    private Rubric rubric;
    private List<RubricCriterion> criteria;
    private DocumentChunk chunk;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        rubric = Rubric.create(UUID.randomUUID(), "R", null);
        criteria = List.of(
                RubricCriterion.create(rubric.getRubricId(), "Chính xác", "0-6", new BigDecimal("6"), List.of(), 1),
                RubricCriterion.create(rubric.getRubricId(), "Lập luận", "0-4", new BigDecimal("4"), List.of(), 2));
        rubric.recalculateTotal(criteria);
        documentId = UUID.randomUUID();
        chunk = DocumentChunk.builder()
                .chunkId(UUID.randomUUID())
                .documentId(documentId)
                .content("Giao dịch đảm bảo tính nguyên tử: hoặc tất cả thao tác thành công, hoặc không thao tác nào.")
                .build();
    }

    private QuestionVersion aiDraft(boolean confirmed) {
        QuestionVersion version = QuestionVersion.newDraft(UUID.randomUUID(), 1, "Câu hỏi?", "Đáp án",
                BloomLevel.UNDERSTAND, QuestionGenerationMode.AI_RAG, UUID.randomUUID());
        version.setBloomConfirmed(confirmed);
        return version;
    }

    private QuestionSource source(String quote) {
        return QuestionSource.builder().chunkId(chunk.getChunkId()).documentId(documentId).citationQuote(quote).build();
    }

    private List<FieldViolation> validate(QuestionVersion version, List<RubricCriterion> criteria,
                                          List<QuestionSource> sources, Set<UUID> readyDocs) {
        return QuestionApprovalPolicy.validate(version, rubric, criteria, sources,
                Map.of(chunk.getChunkId(), chunk), readyDocs);
    }

    @Test
    @DisplayName("valid AI draft with confirmed Bloom, consistent rubric and grounded source passes")
    void validDraftPasses() {
        assertThat(validate(aiDraft(true), criteria, List.of(source("tính   NGUYÊN tử")), Set.of(documentId))).isEmpty();
    }

    @Test
    @DisplayName("BR-01: AI Bloom level must be confirmed")
    void bloomMustBeConfirmed() {
        List<FieldViolation> violations = validate(aiDraft(false), criteria, List.of(source("nguyên tử")), Set.of(documentId));
        assertThat(violations).extracting(FieldViolation::code).containsExactly(ErrorCode.BLOOM_NOT_CONFIRMED);
        assertThat(violations.get(0).field()).isEqualTo("bloomConfirmed");
    }

    @Test
    @DisplayName("BR-02: total mismatch, missing criteria")
    void rubricRules() {
        rubric.setTotalPoints(new BigDecimal("12"));
        assertThat(validate(aiDraft(true), criteria, List.of(source("nguyên tử")), Set.of(documentId)))
                .extracting(FieldViolation::field, FieldViolation::code)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("rubric.totalScore", ErrorCode.RUBRIC_SCORE_MISMATCH));

        assertThat(validate(aiDraft(true), List.of(), List.of(source("nguyên tử")), Set.of(documentId)))
                .extracting(FieldViolation::code).containsExactly(ErrorCode.RUBRIC_REQUIRED);
    }

    @Test
    @DisplayName("BR-03: AI draft needs a grounded source in a READY document")
    void sourceRules() {
        assertThat(validate(aiDraft(true), criteria, List.of(), Set.of(documentId)))
                .extracting(FieldViolation::code).containsExactly(ErrorCode.SOURCE_REQUIRED);
        assertThat(validate(aiDraft(true), criteria, List.of(source("không có trong tài liệu")), Set.of(documentId)))
                .extracting(FieldViolation::code).containsExactly(ErrorCode.INVALID_CITATION_QUOTE);
        assertThat(validate(aiDraft(true), criteria, List.of(source("nguyên tử")), Set.of()))
                .extracting(FieldViolation::code).containsExactly(ErrorCode.SOURCE_REQUIRED);
        QuestionSource missingChunk = QuestionSource.builder().chunkId(UUID.randomUUID()).citationQuote("x").build();
        assertThat(validate(aiDraft(true), criteria, List.of(missingChunk), Set.of(documentId)))
                .extracting(FieldViolation::code).containsExactly(ErrorCode.SOURCE_REQUIRED);
    }

    @Test
    @DisplayName("manual drafts need no source; every violation is reported at once")
    void manualDraftAndAllViolations() {
        QuestionVersion manual = QuestionVersion.newDraft(UUID.randomUUID(), 1, " ", null, BloomLevel.APPLY,
                QuestionGenerationMode.MANUAL, UUID.randomUUID());
        rubric.setTotalPoints(BigDecimal.ONE);
        assertThat(validate(manual, criteria, List.of(), Set.of()))
                .extracting(FieldViolation::field)
                .containsExactly("content", "expectedAnswer", "rubric.totalScore");
    }

    @Test
    void nearDuplicateDetection() {
        List<Set<String>> existing = List.of(QuestionSimilarity.tokens("Trình bày 4 tính chất ACID của giao dịch"));
        assertThat(QuestionSimilarity.isNearDuplicate("Trình bày bốn tính chất ACID của giao dịch?", existing, 0.7)).isTrue();
        assertThat(QuestionSimilarity.isNearDuplicate("Phân tích chỉ mục B-tree trong PostgreSQL", existing, 0.7)).isFalse();
        assertThat(QuestionSimilarity.jaccard(Set.of(), Set.of("a"))).isZero();
    }
}
