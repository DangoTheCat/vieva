package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
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

    List<CourseDocumentJpaEntity> findBySubject_SubjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID subjectId);

    Optional<CourseDocumentJpaEntity> findByDocumentIdAndDeletedAtIsNull(UUID documentId);

    List<CourseDocumentJpaEntity> findByDocumentIdInAndDeletedAtIsNull(Collection<UUID> documentIds);

    @Query("SELECT d FROM CourseDocumentJpaEntity d WHERE d.indexingStatus = com.example.vieva.domain.entities.DocumentIndexingStatus.INDEXING AND d.updatedAt < :threshold AND d.deletedAt IS NULL")
    List<CourseDocumentJpaEntity> findStaleIndexing(@Param("threshold") Instant threshold);

    @Query("SELECT d FROM CourseDocumentJpaEntity d WHERE d.indexingStatus = com.example.vieva.domain.entities.DocumentIndexingStatus.UPLOADED AND d.updatedAt < :threshold AND d.deletedAt IS NULL")
    List<CourseDocumentJpaEntity> findStaleUploaded(@Param("threshold") Instant threshold);
}
