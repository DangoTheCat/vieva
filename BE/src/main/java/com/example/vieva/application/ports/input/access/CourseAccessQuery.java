package com.example.vieva.application.ports.input.access;

import java.util.UUID;

public interface CourseAccessQuery {
    boolean canAccessSubject(UUID subjectId, UUID actorId, boolean isAdmin);
    boolean canAccessDocument(UUID documentId, UUID actorId, boolean isAdmin);
    boolean canAccessQuestion(UUID questionId, UUID actorId, boolean isAdmin);
    boolean canAccessVersion(UUID versionId, UUID actorId, boolean isAdmin);
    boolean canAccessGenerationRequest(UUID requestId, UUID actorId, boolean isAdmin);
}
