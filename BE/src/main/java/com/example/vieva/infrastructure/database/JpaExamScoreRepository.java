package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JpaExamScoreRepository extends JpaRepository<ExamScoreJpaEntity, UUID>, JpaSpecificationExecutor<ExamScoreJpaEntity> {
    Optional<ExamScoreJpaEntity> findByAttemptId(UUID attemptId);
}
