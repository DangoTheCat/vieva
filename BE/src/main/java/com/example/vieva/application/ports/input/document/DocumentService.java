package com.example.vieva.application.ports.input.document;

import java.util.List;
import java.util.UUID;

public interface DocumentService {
    void archiveDocument(UUID documentId, UUID actorId);

    CourseDocumentResult processDocumentUpload(UUID subjectId, String filename, byte[] content, UUID actorId);

    CourseDocumentResult uploadDocument(UUID subjectId, String filename, byte[] content, UUID actorId);

    CourseDocumentResult retryIndexing(UUID documentId, UUID actorId);

    List<CourseDocumentResult> getDocumentsBySubject(UUID subjectId);

    CourseDocumentResult getDocumentById(UUID documentId);

    DocumentPreview previewDocument(UUID documentId, UUID actorId);

    void softDeleteDocument(UUID documentId, UUID actorId);

    CourseDocumentResult updateDocumentMetadata(UUID documentId, UUID topicId, String metadataJson, UUID actorId);
}
