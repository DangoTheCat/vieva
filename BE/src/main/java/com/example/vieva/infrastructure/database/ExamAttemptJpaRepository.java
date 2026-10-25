package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.AttemptStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamAttemptJpaRepository extends JpaRepository<ExamAttemptJpaEntity, UUID>, JpaSpecificationExecutor<ExamAttemptJpaEntity> {
    List<ExamAttemptJpaEntity> findByStudentIdAndStatus(UUID studentId, AttemptStatus status);
    List<ExamAttemptJpaEntity> findByStatus(AttemptStatus status);
    List<ExamAttemptJpaEntity> findByStudentIdOrderByStartedAtDesc(UUID studentId);
    Optional<ExamAttemptJpaEntity> findByExamIdAndStudentId(UUID examId, UUID studentId);
    long countByExamId(UUID examId);
    List<ExamAttemptJpaEntity> findByExamId(UUID examId);
    long countByStatus(AttemptStatus status);
    org.springframework.data.domain.Page<ExamAttemptJpaEntity> findByStatusOrderByCompletedAtDesc(
            AttemptStatus status, org.springframework.data.domain.Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ExamAttemptJpaEntity a WHERE a.id = :id")
    Optional<ExamAttemptJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
