package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tài liệu giáo trình/slide do GV tải lên; chỉ READY mới được dùng cho RAG.
 * Lifecycle: UPLOADED → INDEXING → READY | FAILED; FAILED → UPLOADED via a bounded retry.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDocument {
    private UUID documentId;
    private UUID subjectId;
    private UUID uploadedBy;
    private String fileName;
    private String fileUrl;
    private String storageKey;
    private Long fileSizeBytes;
    private String mimeType;
    private DocumentIndexingStatus indexingStatus;
    private String errorMessage;
    private String extractedTextSummary;
    private Integer totalChunks;
    private Integer indexAttempts;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public boolean isReady() {
        return indexingStatus == DocumentIndexingStatus.READY && deletedAt == null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** UPLOADED → INDEXING. Counts the attempt so retries stay bounded. */
    public void startIndexing() {
        if (indexingStatus != DocumentIndexingStatus.UPLOADED) {
            throw new AppException(ErrorCode.INVALID_DOCUMENT_STATE,
                    "Only UPLOADED documents can start indexing (current: " + indexingStatus + ")");
        }
        this.indexingStatus = DocumentIndexingStatus.INDEXING;
        this.indexAttempts = (indexAttempts == null ? 0 : indexAttempts) + 1;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    /** INDEXING → READY. */
    public void markReady(int chunkCount, String summary) {
        if (indexingStatus != DocumentIndexingStatus.INDEXING) {
            throw new AppException(ErrorCode.INVALID_DOCUMENT_STATE,
                    "Only INDEXING documents can become READY (current: " + indexingStatus + ")");
        }
        if (chunkCount <= 0) {
            throw new AppException(ErrorCode.INVALID_DOCUMENT_STATE, "A READY document needs at least one chunk");
        }
        this.indexingStatus = DocumentIndexingStatus.READY;
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
        if (indexingStatus == DocumentIndexingStatus.READY) {
            throw new AppException(ErrorCode.INVALID_DOCUMENT_STATE, "A READY document cannot be marked as FAILED");
        }
        this.indexingStatus = DocumentIndexingStatus.FAILED;
        this.errorMessage = reason;
        this.totalChunks = 0;
        this.updatedAt = Instant.now();
    }

    /** FAILED → UPLOADED, only while the retry budget is not exhausted. */
    public void requeueForRetry(int maxAttempts) {
        if (indexingStatus != DocumentIndexingStatus.FAILED) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_RETRYABLE,
                    "Only FAILED documents can be re-indexed (current: " + indexingStatus + ")");
        }
        int attempts = indexAttempts == null ? 0 : indexAttempts;
        if (attempts >= maxAttempts) {
            throw new AppException(ErrorCode.RETRY_LIMIT_EXCEEDED,
                    "Indexing retry limit reached (" + maxAttempts + " attempts)");
        }
        this.indexingStatus = DocumentIndexingStatus.UPLOADED;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = this.deletedAt;
    }
}
