package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.CourseDocument;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseDocumentRepository {
    CourseDocument save(CourseDocument doc);
    List<CourseDocument> saveAll(List<CourseDocument> docs);
    /** Excludes soft-deleted documents. */
    Optional<CourseDocument> findById(UUID id);
    /** Same as {@link #findById} but takes a row write-lock (SELECT ... FOR UPDATE). */
    Optional<CourseDocument> findByIdForUpdate(UUID id);
    /** Excludes soft-deleted documents. */
    List<CourseDocument> findAllByIds(Collection<UUID> ids);
    List<CourseDocument> findActiveBySubjectId(UUID subjectId);
    List<CourseDocument> findStaleIndexingDocuments(Instant threshold);
    List<CourseDocument> findStaleUploadedDocuments(Instant threshold);
}
