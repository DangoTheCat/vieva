package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamAttemptQuestionSnapshotJpaRepository extends JpaRepository<ExamAttemptQuestionSnapshotJpaEntity, UUID> {
    List<ExamAttemptQuestionSnapshotJpaEntity> findByAttemptIdOrderByOrderIndexAsc(UUID attemptId);
    Optional<ExamAttemptQuestionSnapshotJpaEntity> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}
