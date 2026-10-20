package com.example.vieva.application.ports.input.subject;

import com.example.vieva.application.ports.input.common.PageResult;

import java.util.UUID;

public interface SubjectService {
    SubjectResult createSubject(CreateSubjectRequest request, UUID currentAdminId);
    SubjectResult updateSubject(UUID subjectId, UpdateSubjectRequest request, UUID currentAdminId);
    void deactivateSubject(UUID subjectId, UUID currentAdminId);
    SubjectResult getSubjectById(UUID subjectId);
    PageResult<SubjectResult> getSubjects(SubjectSearchCriteria criteria);
}
