package com.example.vieva.adapters.presenters;

import com.example.vieva.domain.entities.CourseDocument;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CourseDocumentPresenter {

    public CourseDocumentDto toDto(CourseDocument document) {
        if (document == null) {
            return null;
        }
        return CourseDocumentDto.builder()
                .documentId(document.getDocumentId())
                .subjectId(document.getSubjectId())
                .uploadedBy(document.getUploadedBy())
                .fileName(document.getFileName())
                .fileUrl(document.getFileUrl())
                .fileSizeBytes(document.getFileSizeBytes())
                .mimeType(document.getMimeType())
                .indexingStatus(document.getIndexingStatus())
                .errorMessage(document.getErrorMessage())
                .totalChunks(document.getTotalChunks())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    public List<CourseDocumentDto> toDtoList(List<CourseDocument> documents) {
        if (documents == null) {
            return Collections.emptyList();
        }
        return documents.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
