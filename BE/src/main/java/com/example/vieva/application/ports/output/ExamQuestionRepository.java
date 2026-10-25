package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.ExamQuestion;

import java.util.List;
import java.util.UUID;

public interface ExamQuestionRepository {
    ExamQuestion save(ExamQuestion examQuestion);
    List<ExamQuestion> saveAll(List<ExamQuestion> examQuestions);
    List<ExamQuestion> findByExamIdOrderByOrderIndexAsc(UUID examId);
    void deleteByExamId(UUID examId);
}
