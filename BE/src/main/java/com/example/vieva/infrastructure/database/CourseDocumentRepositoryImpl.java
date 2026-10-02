package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.domain.entities.CourseDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CourseDocumentRepositoryImpl implements CourseDocumentRepository {

    private final CourseDocumentJpaRepository jpaRepository;
    private final CourseDocumentPersistenceMapper mapper;

    @Override
    public CourseDocument save(CourseDocument doc) {
        CourseDocumentJpaEntity entity = mapper.toEntity(doc);
        CourseDocumentJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<CourseDocument> saveAll(List<CourseDocument> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }
        List<CourseDocumentJpaEntity> entities = docs.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CourseDocument> findById(UUID id) {
        return jpaRepository.findByDocumentIdAndDeletedAtIsNull(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<CourseDocument> findByIdForUpdate(UUID id) {
        return jpaRepository.lockById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<CourseDocument> findActiveBySubjectId(UUID subjectId) {
        return jpaRepository.findBySubject_SubjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(subjectId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseDocument> findAllByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByDocumentIdInAndDeletedAtIsNull(ids).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseDocument> findStaleIndexingDocuments(Instant threshold) {
        return jpaRepository.findStaleIndexing(threshold).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseDocument> findStaleUploadedDocuments(Instant threshold) {
        return jpaRepository.findStaleUploaded(threshold).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
