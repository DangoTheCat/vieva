package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.RubricCriterionRepository;
import com.example.vieva.domain.entities.RubricCriterion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RubricCriterionRepositoryImpl implements RubricCriterionRepository {

    private final RubricCriterionJpaRepository jpaRepository;
    private final RubricPersistenceMapper mapper;

    @Override
    public RubricCriterion save(RubricCriterion criterion) {
        RubricCriterionJpaEntity entity = mapper.toEntity(criterion);
        RubricCriterionJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<RubricCriterion> saveAll(List<RubricCriterion> criteria) {
        List<RubricCriterionJpaEntity> entities = criteria.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RubricCriterion> findByRubricId(UUID rubricId) {
        return jpaRepository.findByRubric_RubricIdOrderByOrderIndexAsc(rubricId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RubricCriterion> findByRubricIds(Collection<UUID> rubricIds) {
        if (rubricIds == null || rubricIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByRubric_RubricIdIn(rubricIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByRubricId(UUID rubricId) {
        jpaRepository.deleteByRubricId(rubricId);
    }
}
