package com.example.vieva.application.usecases.document;

import com.example.vieva.domain.entities.CourseDocument;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DocumentIndexingService {
    CourseDocument uploadDocument(UUID subjectId, String filename, byte[] content, String mimeType, UUID uploadedBy);
    void indexDocument(UUID documentId, byte[] fileBytes);
    List<CourseDocument> getDocumentsBySubject(UUID subjectId);
    CourseDocument getDocumentById(UUID documentId);
    void softDeleteDocument(UUID documentId, UUID currentUserId);
    /**
     * Marks documents stuck in INDEXING/UPLOADED beyond the given thresholds as FAILED.
     *
     * @return number of documents marked failed
     */
    int failStaleDocuments(Instant indexingThreshold, Instant uploadedThreshold);
}
