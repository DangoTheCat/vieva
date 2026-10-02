package com.example.vieva.infrastructure.security;

import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.QuestionGenerationRequestRepository;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationRequest;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * HTTP-level course-scoped RBAC used from {@code @PreAuthorize} (BR-06). Every lookup resolves the
 * owning subject and checks an active lecturer assignment; ADMIN has global access.
 * Unknown resources return {@code true} so the use case can answer 404 instead of a misleading 403.
 * Write use cases re-check inside their transaction ({@code SubjectAccessGuard}).
 */
@Component("courseSecurityEvaluator")
@RequiredArgsConstructor
public class CourseSecurityEvaluator {

    private final LecturerSubjectRepository lecturerSubjectRepository;
    private final CourseDocumentRepository courseDocumentRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionGenerationRequestRepository generationRequestRepository;

    public boolean canAccessSubject(UUID subjectId, Authentication authentication) {
        if (subjectId == null || !isAuthenticated(authentication)) {
            return false;
        }
        if (isAdmin(authentication)) {
            return true;
        }
        UUID userId = extractUserId(authentication);
        return userId != null && lecturerSubjectRepository.isLecturerAssignedToSubject(userId, subjectId);
    }

    public boolean canAccessDocument(UUID documentId, Authentication authentication) {
        if (documentId == null || !isAuthenticated(authentication)) {
            return false;
        }
        return courseDocumentRepository.findById(documentId)
                .map(CourseDocument::getSubjectId)
                .map(subjectId -> canAccessSubject(subjectId, authentication))
                .orElse(true);
    }

    public boolean canAccessQuestion(UUID questionId, Authentication authentication) {
        if (questionId == null || !isAuthenticated(authentication)) {
            return false;
        }
        return questionRepository.findById(questionId)
                .map(Question::getSubjectId)
                .map(subjectId -> canAccessSubject(subjectId, authentication))
                .orElse(true);
    }

    public boolean canAccessQuestionVersion(UUID versionId, Authentication authentication) {
        if (versionId == null || !isAuthenticated(authentication)) {
            return false;
        }
        return questionVersionRepository.findById(versionId)
                .map(QuestionVersion::getQuestionId)
                .map(questionId -> canAccessQuestion(questionId, authentication))
                .orElse(true);
    }

    public boolean canAccessGenerationRequest(UUID requestId, Authentication authentication) {
        if (requestId == null || !isAuthenticated(authentication)) {
            return false;
        }
        return generationRequestRepository.findById(requestId)
                .map(QuestionGenerationRequest::getSubjectId)
                .map(subjectId -> canAccessSubject(subjectId, authentication))
                .orElse(true);
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private static UUID extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof User user) {
            return user.getUserId();
        }
        return null;
    }
}
