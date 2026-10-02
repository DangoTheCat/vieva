package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.CourseDocument;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CourseDocumentPersistenceMapper {

    public CourseDocument toDomain(CourseDocumentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return CourseDocument.builder()
                .documentId(entity.getDocumentId())
                .subjectId(entity.getSubject() != null ? entity.getSubject().getSubjectId() : null)
                .uploadedBy(entity.getUploadedBy())
                .fileName(entity.getFileName())
                .fileUrl(entity.getFileUrl())
                .fileSizeBytes(entity.getFileSizeBytes())
                .mimeType(entity.getMimeType())
                .indexingStatus(entity.getIndexingStatus())
                .errorMessage(entity.getErrorMessage())
                .extractedTextSummary(entity.getExtractedTextSummary())
                .totalChunks(entity.getTotalChunks())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public CourseDocumentJpaEntity toEntity(CourseDocument domain) {
        if (domain == null) {
            return null;
        }
        SubjectJpaEntity subjectEntity = null;
        if (domain.getSubjectId() != null) {
            subjectEntity = SubjectJpaEntity.builder()
                    .subjectId(domain.getSubjectId())
                    .build();
        }

        return CourseDocumentJpaEntity.builder()
                .documentId(domain.getDocumentId() != null ? domain.getDocumentId() : UUID.randomUUID())
                .subject(subjectEntity)
                .uploadedBy(domain.getUploadedBy())
                .fileName(domain.getFileName())
                .fileUrl(domain.getFileUrl())
                .fileSizeBytes(domain.getFileSizeBytes())
                .mimeType(domain.getMimeType())
                .indexingStatus(domain.getIndexingStatus())
                .errorMessage(domain.getErrorMessage())
                .extractedTextSummary(domain.getExtractedTextSummary())
                .totalChunks(domain.getTotalChunks() != null ? domain.getTotalChunks() : 0)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .isNew(domain.getCreatedAt() == null)
                .build();
    }
}
