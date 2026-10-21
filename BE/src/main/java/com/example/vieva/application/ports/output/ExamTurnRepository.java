package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.ExamTurn;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamTurnRepository {
    ExamTurn save(ExamTurn turn);
    Optional<ExamTurn> findById(UUID id);
    List<ExamTurn> findByAttemptIdOrderByTurnOrderAsc(UUID attemptId);
    List<ExamTurn> findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(UUID attemptId, UUID questionId);
}
