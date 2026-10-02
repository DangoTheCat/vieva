package com.example.vieva.application.ports.output;

import com.example.vieva.application.ports.input.SubjectSearchCriteria;
import com.example.vieva.domain.entities.Subject;

import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository {
    Subject save(Subject subject);
    Optional<Subject> findById(UUID subjectId);
    Optional<Subject> findBySubjectCode(String subjectCode);
    boolean existsBySubjectCode(String subjectCode);
    PagedResult<Subject> findAll(SubjectSearchCriteria criteria);
}
