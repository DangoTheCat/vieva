package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.DomainErrorCode;
import com.example.vieva.domain.exception.DomainException;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tài liệu giáo trình/slide do GV tải lên; chỉ READY mới được dùng cho RAG.
 * Lifecycle: UPLOADED → INDEXING → READY | FAILED; FAILED → UPLOADED via a bounded retry.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CourseDocument {
    private UUID documentId;
    private UUID subjectId;
    private UUID topicId;
    private UUID uploadedBy;
    private String fileName;
    private String fileUrl;
    private String storageKey;
    private Long fileSizeBytes;
    private String mimeType;
    private DocumentStatus indexingStatus;
    private String errorMessage;
    private String extractedTextSummary;
    private Integer totalChunks;
    private Integer indexAttempts;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public boolean isReady() {
        return indexingStatus == DocumentStatus.READY && deletedAt == null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isAvailableAsEvidence() {
        return deletedAt == null && (indexingStatus == DocumentStatus.READY || indexingStatus == DocumentStatus.ARCHIVED);
    }

    public void archive() {
        if (!isReady()) throw new DomainException(DomainErrorCode.INVALID_DOCUMENT_STATE, "Only READY documents can be archived");
        indexingStatus = DocumentStatus.ARCHIVED;
        updatedAt = Instant.now();
    }

    /** UPLOADED → INDEXING. Counts the attempt so retries stay bounded. */
    public void startIndexing() {
        if (indexingStatus != DocumentStatus.UPLOADED) {
            throw new DomainException(DomainErrorCode.INVALID_DOCUMENT_STATE,
                    "Only UPLOADED documents can start indexing (current: " + indexingStatus + ")");
        }
        this.indexingStatus = DocumentStatus.INDEXING;
        this.indexAttempts = (indexAttempts == null ? 0 : indexAttempts) + 1;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    /** INDEXING → READY. */
    public void markReady(int chunkCount, String summary) {
        if (indexingStatus != DocumentStatus.INDEXING) {
            throw new DomainException(DomainErrorCode.INVALID_DOCUMENT_STATE,
                    "Only INDEXING documents can become READY (current: " + indexingStatus + ")");
        }
        if (chunkCount <= 0) {
            throw new DomainException(DomainErrorCode.INVALID_DOCUMENT_STATE, "A READY document needs at least one chunk");
        }
        this.indexingStatus = DocumentStatus.READY;
        this.totalChunks = chunkCount;
        this.extractedTextSummary = summary;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    /**
     * Any non-READY state → FAILED with a reason. A READY document never falls back to FAILED,
     * because its chunks may already be cited by questions.
     */
    public void markFailed(String reason) {
        if (indexingStatus == DocumentStatus.READY || indexingStatus == DocumentStatus.ARCHIVED) {
            throw new DomainException(DomainErrorCode.INVALID_DOCUMENT_STATE, "A READY document cannot be marked as FAILED");
        }
        this.indexingStatus = DocumentStatus.FAILED;
        this.errorMessage = reason;
        this.totalChunks = 0;
        this.updatedAt = Instant.now();
    }

    /** FAILED → UPLOADED, only while the retry budget is not exhausted. */
    public void requeueForRetry(int maxAttempts) {
        if (indexingStatus != DocumentStatus.FAILED) {
            throw new DomainException(DomainErrorCode.DOCUMENT_NOT_RETRYABLE,
                    "Only FAILED documents can be re-indexed (current: " + indexingStatus + ")");
        }
        int attempts = indexAttempts == null ? 0 : indexAttempts;
        if (attempts >= maxAttempts) {
            throw new DomainException(DomainErrorCode.RETRY_LIMIT_EXCEEDED,
                    "Indexing retry limit reached (" + maxAttempts + " attempts)");
        }
        this.indexingStatus = DocumentStatus.UPLOADED;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = this.deletedAt;
    }
}
