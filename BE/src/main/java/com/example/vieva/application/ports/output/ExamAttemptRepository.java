package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptRepository {
    ExamAttempt save(ExamAttempt attempt);
    Optional<ExamAttempt> findById(UUID id);
    List<ExamAttempt> findByStudentAndStatus(UUID studentId, AttemptStatus status);
    List<ExamAttempt> findByStudentId(UUID studentId);
}
