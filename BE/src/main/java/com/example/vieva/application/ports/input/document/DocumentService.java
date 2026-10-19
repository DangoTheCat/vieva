package com.example.vieva.application.ports.input.document;

import com.example.vieva.application.ports.output.DocumentPreview;
import com.example.vieva.domain.entities.CourseDocument;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DocumentService {
    void archiveDocument(UUID documentId, UUID actorId);

    CourseDocument processDocumentUpload(UUID subjectId, String filename, byte[] content, UUID actorId);

    CourseDocument uploadDocument(UUID subjectId, String filename, byte[] content, UUID actorId);

    void indexDocument(UUID documentId);

    CourseDocument retryIndexing(UUID documentId, UUID actorId);

    List<CourseDocument> getDocumentsBySubject(UUID subjectId);

    CourseDocument getDocumentById(UUID documentId);

    DocumentPreview previewDocument(UUID documentId, UUID actorId);

    void softDeleteDocument(UUID documentId, UUID actorId);

    int failStaleDocuments(Instant indexingThreshold, Instant uploadedThreshold);
}
