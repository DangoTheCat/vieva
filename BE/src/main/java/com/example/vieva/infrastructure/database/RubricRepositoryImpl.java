package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.domain.entities.Rubric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RubricRepositoryImpl implements RubricRepository {

    private final RubricJpaRepository jpaRepository;
    private final RubricPersistenceMapper mapper;

    @Override
    public Rubric save(Rubric rubric) {
        RubricJpaEntity entity = mapper.toEntity(rubric);
        RubricJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Rubric> saveAll(List<Rubric> rubrics) {
        if (rubrics == null || rubrics.isEmpty()) {
            return List.of();
        }
        List<RubricJpaEntity> entities = rubrics.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Rubric> findById(UUID rubricId) {
        return jpaRepository.findById(rubricId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Rubric> findByQuestionVersionId(UUID questionVersionId) {
        return jpaRepository.findByQuestionVersion_QuestionVersionId(questionVersionId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Rubric> findByQuestionVersionIds(Collection<UUID> questionVersionIds) {
        if (questionVersionIds == null || questionVersionIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByQuestionVersion_QuestionVersionIdIn(questionVersionIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByQuestionVersionId(UUID questionVersionId) {
        jpaRepository.deleteByVersionId(questionVersionId);
    }
}
