package com.example.vieva.domain.entities;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tài liệu giáo trình/slide do GV tải lên; chỉ READY mới được dùng cho RAG.
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
    private Long fileSizeBytes;
    private String mimeType;
    private DocumentIndexingStatus indexingStatus;
    private String errorMessage;
    private String extractedTextSummary;
    private Integer totalChunks;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    /**
     * Domain lifecycle transition: mark the document as failed with a reason.
     * Keeps the failure invariant (status + message + timestamp) inside the entity
     * instead of spreading setters across services/schedulers.
     */
    public void markFailed(String reason) {
        this.indexingStatus = DocumentIndexingStatus.FAILED;
        this.errorMessage = reason;
        this.updatedAt = Instant.now();
    }
}
