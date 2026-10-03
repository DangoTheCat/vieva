package com.example.vieva.application.ports.output;

import com.example.vieva.application.ports.input.SubjectSearchCriteria;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository {
    Subject save(Subject subject);
    Optional<Subject> findById(UUID subjectId);
    List<Subject> findAllByIds(Collection<UUID> subjectIds);
    Optional<Subject> findBySubjectCode(String subjectCode);
    boolean existsBySubjectCode(String subjectCode);
    PagedResult<Subject> findAll(SubjectSearchCriteria criteria);
    List<Subject> findAllByStatus(SubjectStatus status);
}
