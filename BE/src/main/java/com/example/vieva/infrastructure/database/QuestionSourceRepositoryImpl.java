package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.domain.entities.QuestionSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class QuestionSourceRepositoryImpl implements QuestionSourceRepository {

    private final QuestionSourceJpaRepository jpaRepository;
    private final QuestionVersionJpaRepository questionVersionJpaRepository;
    private final QuestionSourcePersistenceMapper mapper;

    @Override
    @Transactional
    public List<QuestionSource> saveAll(List<QuestionSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        List<QuestionSourceJpaEntity> entities = sources.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionSource> findByQuestionVersionId(UUID questionVersionId) {
        return jpaRepository.findByQuestionVersion_QuestionVersionIdOrderBySourceOrderAsc(questionVersionId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionSource> findByQuestionVersionIds(Collection<UUID> questionVersionIds) {
        if (questionVersionIds == null || questionVersionIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByQuestionVersion_QuestionVersionIdIn(questionVersionIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByDocumentId(UUID documentId) {
        return jpaRepository.existsByDocumentId(documentId);
    }

    @Override
    public void deleteByQuestionVersionId(UUID questionVersionId) {
        jpaRepository.deleteByVersionId(questionVersionId);
    }

    /** The owning version is versioned (@Version): reference it instead of an id-only stub. */
    private QuestionSourceJpaEntity toEntity(QuestionSource source) {
        QuestionSourceJpaEntity entity = mapper.toEntity(source);
        if (source.getQuestionVersionId() != null) {
            entity.setQuestionVersion(questionVersionJpaRepository.getReferenceById(source.getQuestionVersionId()));
        }
        return entity;
    }
}
