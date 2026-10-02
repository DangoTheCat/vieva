package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.QuestionSource;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface QuestionSourceRepository {
    QuestionSource save(QuestionSource source);
    List<QuestionSource> saveAll(List<QuestionSource> sources);
    List<QuestionSource> findByQuestionVersionId(UUID questionVersionId);
    List<QuestionSource> findByQuestionVersionIds(Collection<UUID> questionVersionIds);
    void deleteByQuestionVersionId(UUID questionVersionId);
}
