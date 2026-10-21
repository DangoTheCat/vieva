package com.example.vieva.infrastructure.security;

import com.example.vieva.application.ports.input.access.CourseAccessQuery;
import com.example.vieva.application.ports.input.common.ActorView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * HTTP-level course-scoped RBAC used from {@code @PreAuthorize} (BR-06).
 * Delegates access checks to the {@link CourseAccessQuery} input port.
 */
@Component("courseSecurityEvaluator")
@RequiredArgsConstructor
public class CourseSecurityEvaluator {

    private final CourseAccessQuery courseAccessQuery;

    public boolean canAccessSubject(UUID subjectId, Authentication authentication) {
        if (subjectId == null || !isAuthenticated(authentication)) {
            return false;
        }
        UUID actorId = extractUserId(authentication);
        return courseAccessQuery.canAccessSubject(subjectId, actorId, isAdmin(authentication));
    }

    public boolean canAccessDocument(UUID documentId, Authentication authentication) {
        if (documentId == null || !isAuthenticated(authentication)) {
            return false;
        }
        UUID actorId = extractUserId(authentication);
        return courseAccessQuery.canAccessDocument(documentId, actorId, isAdmin(authentication));
    }

    public boolean canAccessQuestion(UUID questionId, Authentication authentication) {
        if (questionId == null || !isAuthenticated(authentication)) {
            return false;
        }
        UUID actorId = extractUserId(authentication);
        return courseAccessQuery.canAccessQuestion(questionId, actorId, isAdmin(authentication));
    }

    public boolean canAccessQuestionVersion(UUID versionId, Authentication authentication) {
        if (versionId == null || !isAuthenticated(authentication)) {
            return false;
        }
        UUID actorId = extractUserId(authentication);
        return courseAccessQuery.canAccessVersion(versionId, actorId, isAdmin(authentication));
    }

    public boolean canAccessGenerationRequest(UUID requestId, Authentication authentication) {
        if (requestId == null || !isAuthenticated(authentication)) {
            return false;
        }
        UUID actorId = extractUserId(authentication);
        return courseAccessQuery.canAccessGenerationRequest(requestId, actorId, isAdmin(authentication));
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private static UUID extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof ActorView actor) {
            return actor.id();
        }
        return null;
    }
}
