package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.RubricTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RubricTemplateRepository {
    RubricTemplate save(RubricTemplate template);
    Optional<RubricTemplate> findById(UUID id);
    List<RubricTemplate> findBySubjectId(UUID subjectId);
    void deleteById(UUID id);
}
