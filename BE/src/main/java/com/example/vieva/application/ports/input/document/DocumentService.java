package com.example.vieva.application.ports.input.document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DocumentService {
    void archiveDocument(UUID documentId, UUID actorId);

    CourseDocumentResult processDocumentUpload(UUID subjectId, String filename, byte[] content, UUID actorId);

    CourseDocumentResult uploadDocument(UUID subjectId, String filename, byte[] content, UUID actorId);

    void indexDocument(UUID documentId);

    CourseDocumentResult retryIndexing(UUID documentId, UUID actorId);

    List<CourseDocumentResult> getDocumentsBySubject(UUID subjectId);

    CourseDocumentResult getDocumentById(UUID documentId);

    DocumentPreview previewDocument(UUID documentId, UUID actorId);

    void softDeleteDocument(UUID documentId, UUID actorId);

    int failStaleDocuments(Instant indexingThreshold, Instant uploadedThreshold);
}
