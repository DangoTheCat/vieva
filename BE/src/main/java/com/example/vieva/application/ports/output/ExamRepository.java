package com.example.vieva.application.ports.output;

import com.example.vieva.domain.entities.Exam;
import com.example.vieva.domain.entities.ExamStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamRepository {
    Exam save(Exam exam);
    Optional<Exam> findById(UUID id);
    Optional<Exam> findByIdForUpdate(UUID id);
    List<Exam> findBySubjectId(UUID subjectId);
    List<Exam> findByStatus(ExamStatus status);
    void deleteById(UUID id);
    long count();
    long countByStatus(ExamStatus status);
}
