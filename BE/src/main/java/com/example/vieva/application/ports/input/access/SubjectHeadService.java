package com.example.vieva.application.ports.input.access;

import com.example.vieva.domain.entities.Subject;

import java.util.UUID;

public interface SubjectHeadService {
    Subject assign(UUID subjectId, UUID lecturerId, UUID adminId);
    UUID headOf(UUID subjectId);
    void requireHead(UUID actorId, UUID subjectId);
}
