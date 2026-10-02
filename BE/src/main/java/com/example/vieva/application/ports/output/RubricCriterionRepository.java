package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.RubricCriterion;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RubricCriterionRepository {
    RubricCriterion save(RubricCriterion criterion);
    List<RubricCriterion> saveAll(List<RubricCriterion> criteria);
    List<RubricCriterion> findByRubricId(UUID rubricId);
    List<RubricCriterion> findByRubricIds(Collection<UUID> rubricIds);
    void deleteByRubricId(UUID rubricId);
}
