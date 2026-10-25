package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.ExamStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamJpaRepository extends JpaRepository<ExamJpaEntity, UUID> {
    List<ExamJpaEntity> findBySubjectId(UUID subjectId);
    List<ExamJpaEntity> findByStatus(ExamStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM ExamJpaEntity e WHERE e.id = :id")
    Optional<ExamJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    long countByStatus(ExamStatus status);
}
