package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionVersionTest {

    private final UUID author = UUID.randomUUID();
    private final UUID reviewer = UUID.randomUUID();

    private QuestionVersion draft(QuestionGenerationMode mode) {
        return QuestionVersion.newDraft(UUID.randomUUID(), 1, "Nội dung câu hỏi", "Đáp án", BloomLevel.APPLY, mode, author);
    }

    private static void assertCode(Runnable action, DomainErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .extracting("errorCode").isEqualTo(code);
    }

    @Nested
    @DisplayName("creation")
    class Creation {
        @Test
        void aiDraftNeedsBloomConfirmation() {
            QuestionVersion version = draft(QuestionGenerationMode.AI_RAG);
            assertThat(version.isDraft()).isTrue();
            assertThat(version.isBloomConfirmed()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = QuestionGenerationMode.class, names = {"MANUAL", "IMPORT"})
        void humanAuthoredDraftIsConfirmed(QuestionGenerationMode mode) {
            assertThat(draft(mode).isBloomConfirmed()).isTrue();
        }
    }

    @Nested
    @DisplayName("state machine DRAFT → APPROVED | REJECTED → (APPROVED → SUPERSEDED)")
    class StateMachine {
        @Test
        void approveDraft() {
            QuestionVersion version = draft(QuestionGenerationMode.MANUAL);
            version.submitForReview();
            version.approve(reviewer);
            assertThat(version.getApprovalStatus()).isEqualTo(QuestionStatus.APPROVED);
            assertThat(version.getReviewedBy()).isEqualTo(reviewer);
            assertThat(version.getReviewedAt()).isNotNull();
        }

        @Test
        void rejectDraftRequiresReason() {
            QuestionVersion version = draft(QuestionGenerationMode.MANUAL);
            version.submitForReview();
            assertCode(() -> version.reject(reviewer, "  "), DomainErrorCode.INVALID_REQUEST);
            version.reject(reviewer, " thiếu căn cứ ");
            assertThat(version.getApprovalStatus()).isEqualTo(QuestionStatus.REJECTED);
            assertThat(version.getRejectionReason()).isEqualTo("thiếu căn cứ");
        }

        @Test
        void approvedVersionIsImmutable() {
            QuestionVersion version = draft(QuestionGenerationMode.MANUAL);
            version.submitForReview();
            version.approve(reviewer);
            assertCode(() -> version.approve(reviewer), DomainErrorCode.VERSION_NOT_UNDER_REVIEW);
            assertCode(() -> version.reject(reviewer, "x"), DomainErrorCode.VERSION_NOT_UNDER_REVIEW);
            assertCode(() -> version.editContent("new", null, null, null), DomainErrorCode.VERSION_NOT_DRAFT);
            assertCode(() -> version.confirmBloom(BloomLevel.CREATE), DomainErrorCode.VERSION_NOT_DRAFT);
        }

        @Test
        void rejectedVersionCannotBeApproved() {
            QuestionVersion version = draft(QuestionGenerationMode.MANUAL);
            version.submitForReview();
            version.reject(reviewer, "sai");
            assertCode(() -> version.approve(reviewer), DomainErrorCode.VERSION_NOT_UNDER_REVIEW);
        }

        @Test
        void onlyApprovedCanBeSuperseded() {
            QuestionVersion version = draft(QuestionGenerationMode.MANUAL);
            assertCode(version::supersede, DomainErrorCode.INVALID_REQUEST);
            version.submitForReview();
            version.approve(reviewer);
            version.supersede();
            assertThat(version.getApprovalStatus()).isEqualTo(QuestionStatus.SUPERSEDED);
        }
    }

    @Nested
    @DisplayName("editing and Bloom confirmation (BR-01)")
    class Editing {
        @Test
        void changingBloomLevelCountsAsConfirmation() {
            QuestionVersion version = draft(QuestionGenerationMode.AI_RAG);
            version.editContent(null, null, BloomLevel.EVALUATE, null);
            assertThat(version.getBloomLevel()).isEqualTo(BloomLevel.EVALUATE);
            assertThat(version.isBloomConfirmed()).isTrue();
        }

        @Test
        void editingTextKeepsConfirmationFlag() {
            QuestionVersion version = draft(QuestionGenerationMode.AI_RAG);
            version.editContent("  Câu mới  ", "Đáp án mới", BloomLevel.APPLY, null);
            assertThat(version.getQuestionContent()).isEqualTo("Câu mới");
            assertThat(version.isBloomConfirmed()).isFalse();
        }

        @Test
        void confirmBloomExplicitly() {
            QuestionVersion version = draft(QuestionGenerationMode.AI_RAG);
            version.confirmBloom(null);
            assertThat(version.isBloomConfirmed()).isTrue();
            assertThat(version.getBloomLevel()).isEqualTo(BloomLevel.APPLY);
        }

        @Test
        void staleLockTokenIsRejected() {
            QuestionVersion version = draft(QuestionGenerationMode.MANUAL);
            version.setVersion(3L);
            version.ensureVersionMatches(null);
            version.ensureVersionMatches(3L);
            assertCode(() -> version.ensureVersionMatches(2L), DomainErrorCode.CONCURRENT_MODIFICATION);
        }
    }

    @Nested
    @DisplayName("Copy-on-Write")
    class CopyOnWrite {
        @Test
        void copiesApprovedVersionIntoNewDraft() {
            QuestionVersion approved = draft(QuestionGenerationMode.AI_RAG);
            approved.confirmBloom(BloomLevel.ANALYZE);
            approved.setGenerationRequestId(UUID.randomUUID());
            approved.submitForReview();
            approved.approve(reviewer);

            QuestionVersion copy = approved.copyAsDraft(2, author);

            assertThat(copy.getQuestionVersionId()).isNotEqualTo(approved.getQuestionVersionId());
            assertThat(copy.getQuestionId()).isEqualTo(approved.getQuestionId());
            assertThat(copy.getVersionNumber()).isEqualTo(2);
            assertThat(copy.getParentVersionId()).isEqualTo(approved.getQuestionVersionId());
            assertThat(copy.getApprovalStatus()).isEqualTo(QuestionStatus.DRAFT);
            assertThat(copy.getQuestionContent()).isEqualTo(approved.getQuestionContent());
            assertThat(copy.getBloomLevel()).isEqualTo(BloomLevel.ANALYZE);
            assertThat(copy.isBloomConfirmed()).isTrue();
            assertThat(copy.getGenerationMode()).isEqualTo(QuestionGenerationMode.AI_RAG);
            assertThat(copy.getVersion()).isNull();
            // the original stays untouched
            assertThat(approved.getApprovalStatus()).isEqualTo(QuestionStatus.APPROVED);
        }

        @Test
        void onlyApprovedVersionCanBeCopied() {
            QuestionVersion version = draft(QuestionGenerationMode.MANUAL);
            assertCode(() -> version.copyAsDraft(2, author), DomainErrorCode.NO_APPROVED_VERSION);
        }
    }

    @Nested
    @DisplayName("regeneration")
    class Regeneration {
        @Test
        void replacesAiDraftAndResetsConfirmation() {
            QuestionVersion version = draft(QuestionGenerationMode.AI_RAG);
            version.confirmBloom(null);
            version.replaceWithRegenerated("Câu thay thế", "Đáp án thay thế", BloomLevel.APPLY);
            assertThat(version.getQuestionContent()).isEqualTo("Câu thay thế");
            assertThat(version.isBloomConfirmed()).isFalse();
            assertThat(version.getRegenerationCount()).isEqualTo(1);
        }

        @Test
        void manualDraftCannotBeRegenerated() {
            QuestionVersion version = draft(QuestionGenerationMode.MANUAL);
            assertCode(() -> version.replaceWithRegenerated("a", "b", BloomLevel.APPLY), DomainErrorCode.REGENERATION_NOT_SUPPORTED);
        }
    }
}
