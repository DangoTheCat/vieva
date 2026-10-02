package com.example.vieva.application.usecases.document;

import com.example.vieva.domain.entities.CourseDocument;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * UC1.1: upload a course document, index it asynchronously (extract → chunk → embed → store vectors),
 * follow its status and retry a FAILED indexing within a bounded budget.
 */
public interface DocumentIndexingService {
    /** Validates and stores the file, saves it as UPLOADED and schedules indexing after commit. */
    CourseDocument uploadDocument(UUID subjectId, String filename, byte[] content, UUID actorId);

    /** Runs the indexing pipeline; called by the async worker. Never throws for pipeline failures. */
    void indexDocument(UUID documentId);

    CourseDocument retryIndexing(UUID documentId, UUID actorId);

    List<CourseDocument> getDocumentsBySubject(UUID subjectId);

    CourseDocument getDocumentById(UUID documentId);

    void softDeleteDocument(UUID documentId, UUID actorId);

    /**
     * Marks documents stuck in INDEXING/UPLOADED beyond the given thresholds as FAILED.
     *
     * @return number of documents marked failed
     */
    int failStaleDocuments(Instant indexingThreshold, Instant uploadedThreshold);
}
