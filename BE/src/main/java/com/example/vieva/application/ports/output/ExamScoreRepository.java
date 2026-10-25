package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.ExamScore;

import java.util.Optional;
import java.util.UUID;

public interface ExamScoreRepository {
    ExamScore save(ExamScore score);
    Optional<ExamScore> findById(UUID id);
    Optional<ExamScore> findByAttemptId(UUID attemptId);
    java.util.List<ExamScore> findAll();
}
