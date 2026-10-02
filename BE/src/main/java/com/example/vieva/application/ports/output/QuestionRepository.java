package com.example.vieva.application.ports.output;

import com.example.vieva.application.ports.input.QuestionSearchCriteria;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {
    Question save(Question question);
    List<Question> saveAll(List<Question> questions);
    Optional<Question> findById(UUID questionId);
    Optional<Question> findByCode(String questionCode);
    boolean existsByCode(String questionCode);
    PagedResult<Question> search(QuestionSearchCriteria criteria);
    void updateStatus(UUID questionId, QuestionStatus status);
    void deleteById(UUID questionId);
}
