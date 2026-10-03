package com.example.vieva.application.ports.output;

import com.example.vieva.application.ports.input.QuestionBankSearchCriteria;
import com.example.vieva.domain.entities.Question;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {
    Question save(Question question);
    List<Question> saveAll(List<Question> questions);
    Optional<Question> findById(UUID questionId);
    List<Question> findAllByIds(Collection<UUID> questionIds);
    /** UC1.4: only questions that have an approved (published) version. */
    PagedResult<Question> searchBank(QuestionBankSearchCriteria criteria);
    void deleteById(UUID questionId);
}
