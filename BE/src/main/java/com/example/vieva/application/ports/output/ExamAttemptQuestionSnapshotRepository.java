package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.ExamAttemptQuestionSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptQuestionSnapshotRepository {
    ExamAttemptQuestionSnapshot save(ExamAttemptQuestionSnapshot snapshot);
    List<ExamAttemptQuestionSnapshot> saveAll(List<ExamAttemptQuestionSnapshot> snapshots);
    Optional<ExamAttemptQuestionSnapshot> findById(UUID id);
    List<ExamAttemptQuestionSnapshot> findByAttemptIdOrderByOrderIndexAsc(UUID attemptId);
    Optional<ExamAttemptQuestionSnapshot> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}
