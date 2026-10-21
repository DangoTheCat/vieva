package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface JpaExamAttemptRepository extends JpaRepository<ExamAttemptJpaEntity, UUID>, JpaSpecificationExecutor<ExamAttemptJpaEntity> {
    List<ExamAttemptJpaEntity> findByStudentIdAndStatus(UUID studentId, AttemptStatus status);
    List<ExamAttemptJpaEntity> findByStudentIdOrderByStartedAtDesc(UUID studentId);
}
