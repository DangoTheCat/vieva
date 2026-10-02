package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.QuestionSourceRepository;
import com.example.vieva.domain.entities.QuestionSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class QuestionSourceRepositoryImpl implements QuestionSourceRepository {

    private final QuestionSourceJpaRepository jpaRepository;
    private final QuestionSourcePersistenceMapper mapper;

    @Override
    public QuestionSource save(QuestionSource source) {
        QuestionSourceJpaEntity entity = mapper.toEntity(source);
        QuestionSourceJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<QuestionSource> saveAll(List<QuestionSource> sources) {
        List<QuestionSourceJpaEntity> entities = sources.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionSource> findByQuestionVersionId(UUID questionVersionId) {
        return jpaRepository.findByQuestionVersion_QuestionVersionId(questionVersionId).stream()
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
    public void deleteByQuestionVersionId(UUID questionVersionId) {
        jpaRepository.deleteByVersionId(questionVersionId);
    }
}
