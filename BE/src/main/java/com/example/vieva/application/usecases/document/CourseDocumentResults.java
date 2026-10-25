package com.example.vieva.application.usecases.document;

import com.example.vieva.application.ports.input.document.CourseDocumentResult;
import com.example.vieva.domain.entities.CourseDocument;

public final class CourseDocumentResults {

    private CourseDocumentResults() {
    }

    public static CourseDocumentResult from(CourseDocument document) {
        if (document == null) {
            return null;
        }
        return CourseDocumentResult.builder()
                .documentId(document.getDocumentId())
                .subjectId(document.getSubjectId())
                .topicId(document.getTopicId())
                .uploadedBy(document.getUploadedBy())
                .fileName(document.getFileName())
                .fileUrl(document.getFileUrl())
                .storageKey(document.getStorageKey())
                .fileSizeBytes(document.getFileSizeBytes())
                .mimeType(document.getMimeType())
                .indexingStatus(document.getIndexingStatus() != null ? document.getIndexingStatus().name() : null)
                .errorMessage(document.getErrorMessage())
                .totalChunks(document.getTotalChunks())
                .indexAttempts(document.getIndexAttempts())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
