package com.example.vieva.application.ports.output;

import com.example.vieva.application.ports.input.common.PageResult;
import com.example.vieva.application.ports.input.subject.SubjectSearchCriteria;
import com.example.vieva.domain.entities.Subject;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository {
    Subject save(Subject subject);
    /**
     * Forces SQL execution inside the call, so unique-index violations surface here
     * instead of at transaction commit (concurrent-creation handling).
     */
    Subject saveAndFlush(Subject subject);
    Optional<Subject> findById(UUID subjectId);
    Optional<Subject> findByIdForUpdate(UUID subjectId);
    List<Subject> findAllByIds(Collection<UUID> subjectIds);
    Optional<Subject> findBySubjectCode(String subjectCode);
    boolean existsBySubjectCode(String subjectCode);
    PageResult<Subject> findAll(SubjectSearchCriteria criteria);
}
