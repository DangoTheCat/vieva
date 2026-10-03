package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.Rubric;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RubricRepository {
    Rubric save(Rubric rubric);
    List<Rubric> saveAll(List<Rubric> rubrics);
    Optional<Rubric> findByQuestionVersionId(UUID questionVersionId);
    List<Rubric> findByQuestionVersionIds(Collection<UUID> questionVersionIds);
    void deleteByQuestionVersionId(UUID questionVersionId);
}
