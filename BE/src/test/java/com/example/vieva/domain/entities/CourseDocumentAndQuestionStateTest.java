package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseDocumentAndQuestionStateTest {

    private static CourseDocument uploaded() {
        return CourseDocument.builder()
                .documentId(UUID.randomUUID())
                .indexingStatus(DocumentStatus.UPLOADED)
                .indexAttempts(0)
                .build();
    }

    private static void assertCode(Runnable action, DomainErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOf(DomainException.class).extracting("errorCode").isEqualTo(code);
    }

    @Test
    @DisplayName("document: UPLOADED → INDEXING → READY")
    void documentHappyPath() {
        CourseDocument document = uploaded();
        document.startIndexing();
        assertThat(document.getIndexingStatus()).isEqualTo(DocumentStatus.INDEXING);
        assertThat(document.getIndexAttempts()).isEqualTo(1);
        assertThat(document.isReady()).isFalse();

        document.markReady(12, "summary");
        assertThat(document.getIndexingStatus()).isEqualTo(DocumentStatus.READY);
        assertThat(document.getTotalChunks()).isEqualTo(12);
        assertThat(document.isReady()).isTrue();
    }

    @Test
    @DisplayName("document: storing the file is not enough — READY needs indexing with chunks")
    void readyRequiresIndexingAndChunks() {
        CourseDocument document = uploaded();
        assertCode(() -> document.markReady(3, "s"), DomainErrorCode.INVALID_DOCUMENT_STATE);
        document.startIndexing();
        assertCode(() -> document.markReady(0, "s"), DomainErrorCode.INVALID_DOCUMENT_STATE);
        assertCode(document::startIndexing, DomainErrorCode.INVALID_DOCUMENT_STATE);
    }

    @Test
    @DisplayName("document: READY never falls back to FAILED; deleted document is not READY")
    void readyIsTerminalForFailures() {
        CourseDocument document = uploaded();
        document.startIndexing();
        document.markReady(1, "s");
        assertCode(() -> document.markFailed("boom"), DomainErrorCode.INVALID_DOCUMENT_STATE);
        document.softDelete();
        assertThat(document.isReady()).isFalse();
    }

    @Test
    @DisplayName("document: FAILED → UPLOADED retry is bounded")
    void retryIsBounded() {
        CourseDocument document = uploaded();
        assertCode(() -> document.requeueForRetry(3), DomainErrorCode.DOCUMENT_NOT_RETRYABLE);
        for (int attempt = 1; attempt <= 3; attempt++) {
            document.startIndexing();
            document.markFailed("Embedding service error");
            assertThat(document.getErrorMessage()).isEqualTo("Embedding service error");
            if (attempt < 3) {
                document.requeueForRetry(3);
                assertThat(document.getIndexingStatus()).isEqualTo(DocumentStatus.UPLOADED);
                assertThat(document.getErrorMessage()).isNull();
            }
        }
        assertCode(() -> document.requeueForRetry(3), DomainErrorCode.RETRY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("question: publish sets the bank pointer; ACTIVE → ARCHIVED blocks further publishing")
    void questionLifecycle() {
        Question question = Question.newDraftOwner(UUID.randomUUID(), null, UUID.randomUUID());
        assertThat(question.getStatus()).isEqualTo(QuestionLifecycleStatus.ACTIVE);
        assertThat(question.isInBank()).isFalse();
        assertThat(question.getQuestionCode()).startsWith("Q-");

        UUID versionId = UUID.randomUUID();
        question.publish(versionId);
        assertThat(question.isInBank()).isTrue();
        assertThat(question.getCurrentApprovedVersionId()).isEqualTo(versionId);

        Instant before = question.getUpdatedAt();
        question.archive();
        assertThat(question.getStatus()).isEqualTo(QuestionLifecycleStatus.ARCHIVED);
        assertThat(question.getUpdatedAt()).isAfterOrEqualTo(before);
        assertCode(question::archive, DomainErrorCode.QUESTION_ARCHIVED);
        assertCode(() -> question.publish(UUID.randomUUID()), DomainErrorCode.QUESTION_ARCHIVED);
    }

    @Test
    @DisplayName("generation request: status follows accepted count; attempts are bounded")
    void generationRequestOutcome() {
        QuestionGenerationRequest request = QuestionGenerationRequest.start(UUID.randomUUID(), null, UUID.randomUUID(),
                java.util.List.of(UUID.randomUUID()),
                BloomDistribution.of(java.util.Map.of(BloomLevel.APPLY, 2, BloomLevel.CREATE, 1), 3), null);
        assertThat(request.getStatus()).isEqualTo(QuestionGenerationStatus.PENDING);

        request.beginAttempt(2);
        request.recordOutcome(0, 2, java.util.List.of("bad"), "LLM error");
        assertThat(request.getStatus()).isEqualTo(QuestionGenerationStatus.FAILED);
        request.recordOutcome(2, 0, null, null);
        assertThat(request.getStatus()).isEqualTo(QuestionGenerationStatus.PARTIAL);
        request.recordOutcome(1, 0, null, null);
        assertThat(request.getStatus()).isEqualTo(QuestionGenerationStatus.COMPLETED);

        request.beginAttempt(2);
        assertCode(() -> request.beginAttempt(2), DomainErrorCode.RETRY_LIMIT_EXCEEDED);
        assertCode(request::ensureRetryable, DomainErrorCode.INVALID_REQUEST);
    }
}
