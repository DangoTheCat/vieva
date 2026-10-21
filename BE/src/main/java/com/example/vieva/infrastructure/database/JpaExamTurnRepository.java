package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface JpaExamTurnRepository extends JpaRepository<ExamTurnJpaEntity, UUID>, JpaSpecificationExecutor<ExamTurnJpaEntity> {
    List<ExamTurnJpaEntity> findByAttemptIdOrderByTurnOrderAsc(UUID attemptId);
    List<ExamTurnJpaEntity> findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(UUID attemptId, UUID questionId);
}
