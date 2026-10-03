package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseDocumentJpaRepository extends JpaRepository<CourseDocumentJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"subject"})
    List<CourseDocumentJpaEntity> findBySubject_SubjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID subjectId);

    @EntityGraph(attributePaths = {"subject"})
    Optional<CourseDocumentJpaEntity> findByDocumentIdAndDeletedAtIsNull(UUID documentId);

    /** Row write-lock used by the async indexing worker to claim a document exactly once. */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM CourseDocumentJpaEntity d WHERE d.documentId = :documentId AND d.deletedAt IS NULL")
    Optional<CourseDocumentJpaEntity> lockById(@Param("documentId") UUID documentId);

    @EntityGraph(attributePaths = {"subject"})
    List<CourseDocumentJpaEntity> findByDocumentIdInAndDeletedAtIsNull(Collection<UUID> documentIds);

    @Query("SELECT d FROM CourseDocumentJpaEntity d WHERE d.indexingStatus = com.example.vieva.domain.entities.DocumentIndexingStatus.INDEXING AND d.updatedAt < :threshold AND d.deletedAt IS NULL")
    List<CourseDocumentJpaEntity> findStaleIndexing(@Param("threshold") Instant threshold);

    @Query("SELECT d FROM CourseDocumentJpaEntity d WHERE d.indexingStatus = com.example.vieva.domain.entities.DocumentIndexingStatus.UPLOADED AND d.updatedAt < :threshold AND d.deletedAt IS NULL")
    List<CourseDocumentJpaEntity> findStaleUploaded(@Param("threshold") Instant threshold);
}
