package com.example.vieva.application.ports.input.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDocumentResult {
    private UUID documentId;
    private UUID subjectId;
    private UUID uploadedBy;
    private String fileName;
    private String fileUrl;
    private String storageKey;
    private Long fileSizeBytes;
    private String mimeType;
    private String indexingStatus;
    private String errorMessage;
    private Integer totalChunks;
    private Integer indexAttempts;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isAvailableAsEvidence() {
        return "READY".equals(indexingStatus) || "ARCHIVED".equals(indexingStatus);
    }
}
