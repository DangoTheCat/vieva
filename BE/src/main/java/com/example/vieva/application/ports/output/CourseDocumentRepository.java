package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.CourseDocument;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseDocumentRepository {
    CourseDocument save(CourseDocument doc);
    List<CourseDocument> saveAll(List<CourseDocument> docs);
    Optional<CourseDocument> findById(UUID id);
    List<CourseDocument> findActiveBySubjectId(UUID subjectId);
    void softDelete(UUID documentId);
    List<CourseDocument> findStaleIndexingDocuments(Instant threshold);
    List<CourseDocument> findStaleUploadedDocuments(Instant threshold);
}
