package com.example.vieva.application.usecases.subject;

import com.example.vieva.application.ports.input.CreateSubjectRequest;
import com.example.vieva.application.ports.input.SubjectSearchCriteria;
import com.example.vieva.application.ports.input.UpdateSubjectRequest;
import com.example.vieva.application.ports.output.PagedResult;
import com.example.vieva.domain.entities.Subject;

import java.util.UUID;

public interface SubjectService {
    Subject createSubject(CreateSubjectRequest request, UUID currentAdminId);
    Subject updateSubject(UUID subjectId, UpdateSubjectRequest request, UUID currentAdminId);
    void deactivateSubject(UUID subjectId, UUID currentAdminId);
    Subject getSubjectById(UUID subjectId);
    PagedResult<Subject> getSubjects(SubjectSearchCriteria criteria);
}
