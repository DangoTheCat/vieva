package com.example.vieva.application.ports.input.access;

import com.example.vieva.application.ports.input.subject.SubjectResult;

import java.util.UUID;

public interface SubjectHeadService {
    SubjectResult assign(UUID subjectId, UUID lecturerId, UUID adminId);
    UUID headOf(UUID subjectId);
    void requireHead(UUID actorId, UUID subjectId);
}
