package com.example.vieva.application.ports.output;

import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptRepository {
    ExamAttempt save(ExamAttempt attempt);
    Optional<ExamAttempt> findById(UUID id);
    Optional<ExamAttempt> findByIdForUpdate(UUID id);
    List<ExamAttempt> findByStudentAndStatus(UUID studentId, AttemptStatus status);
    List<ExamAttempt> findByStatus(AttemptStatus status);
    List<ExamAttempt> findByStudentId(UUID studentId);
    Optional<ExamAttempt> findByExamIdAndStudentId(UUID examId, UUID studentId);
    long countByExamId(UUID examId);
    List<ExamAttempt> findByExamId(UUID examId);
    long count();
    long countByStatus(AttemptStatus status);
    PageResult<ExamAttempt> findCompletedAttempts(int page, int size);
    PageResult<ExamAttempt> findAttemptsWithFilters(UUID studentId, UUID subjectId, UUID examId, AttemptStatus status, int page, int size);
}
