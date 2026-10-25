package com.example.vieva.adapters.presenters;

import com.example.vieva.application.ports.input.document.CourseDocumentResult;
import com.example.vieva.application.ports.input.document.DocumentPreview;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CourseDocumentPresenter {

    public CourseDocumentDto toDto(CourseDocumentResult document) {
        if (document == null) {
            return null;
        }
        return CourseDocumentDto.builder()
                .documentId(document.getDocumentId())
                .subjectId(document.getSubjectId())
                .topicId(document.getTopicId())
                .uploadedBy(document.getUploadedBy())
                .fileName(document.getFileName())
                .fileUrl(document.getFileUrl())
                .fileSizeBytes(document.getFileSizeBytes())
                .mimeType(document.getMimeType())
                .indexingStatus(document.getIndexingStatus())
                .errorMessage(document.getErrorMessage())
                .totalChunks(document.getTotalChunks())
                .indexAttempts(document.getIndexAttempts())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    public List<CourseDocumentDto> toDtoList(List<CourseDocumentResult> documents) {
        if (documents == null) {
            return Collections.emptyList();
        }
        return documents.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public DocumentPreviewDto toPreviewDto(DocumentPreview preview) {
        if (preview == null) {
            return null;
        }
        List<DocumentPreviewDto.PreviewChunkDto> chunkDtos = preview.chunks() == null ? Collections.emptyList() :
                preview.chunks().stream()
                        .map(c -> new DocumentPreviewDto.PreviewChunkDto(c.label(), c.text(), c.truncated()))
                        .collect(Collectors.toList());
        return DocumentPreviewDto.builder()
                .document(toDto(preview.document()))
                .chunks(chunkDtos)
                .build();
    }
}
