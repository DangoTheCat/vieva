package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.RubricTemplateRepository;
import com.example.vieva.domain.entities.RubricTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RubricTemplateRepositoryImpl implements RubricTemplateRepository {

    private final RubricTemplateJpaRepository jpaRepository;
    private final RubricTemplatePersistenceMapper mapper;

    @Override
    public RubricTemplate save(RubricTemplate template) {
        RubricTemplateJpaEntity entity = mapper.toEntity(template);
        RubricTemplateJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RubricTemplate> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<RubricTemplate> findBySubjectId(UUID subjectId) {
        return jpaRepository.findBySubjectIdOrderByCreatedAtDesc(subjectId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
