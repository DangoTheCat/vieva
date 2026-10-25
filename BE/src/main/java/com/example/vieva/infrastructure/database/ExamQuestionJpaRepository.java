package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExamQuestionJpaRepository extends JpaRepository<ExamQuestionJpaEntity, UUID> {
    List<ExamQuestionJpaEntity> findByExamIdOrderByOrderIndexAsc(UUID examId);
    void deleteByExamId(UUID examId);
}
