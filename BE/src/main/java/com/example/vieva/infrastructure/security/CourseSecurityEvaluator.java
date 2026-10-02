package com.example.vieva.infrastructure.security;

import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Topic;
import com.example.vieva.domain.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component("courseSecurity")
@RequiredArgsConstructor
public class CourseSecurityEvaluator {

    private final LecturerSubjectRepository lecturerSubjectRepository;
    private final CourseDocumentRepository courseDocumentRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final TopicRepository topicRepository;

    public boolean canAccessSubject(UUID subjectId, Authentication authentication) {
        if (subjectId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // 1. ADMIN has global access
        if (isAdmin(authentication)) {
            return true;
        }

        // 2. LECTURER must be actively assigned to the subject
        UUID userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }

        return lecturerSubjectRepository.isLecturerAssignedToSubject(userId, subjectId);
    }

    public boolean canAccessDocument(UUID documentId, Authentication authentication) {
        if (documentId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        Optional<CourseDocument> docOpt = courseDocumentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            return false;
        }

        return canAccessSubject(docOpt.get().getSubjectId(), authentication);
    }

    public boolean canAccessQuestion(UUID questionId, Authentication authentication) {
        if (questionId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        Optional<Question> questionOpt = questionRepository.findById(questionId);
        if (questionOpt.isEmpty()) {
            return false;
        }

        UUID topicId = questionOpt.get().getTopicId();
        if (topicId == null) {
            return false;
        }

        Optional<Topic> topicOpt = topicRepository.findById(topicId);
        if (topicOpt.isEmpty()) {
            return false;
        }

        return canAccessSubject(topicOpt.get().getSubjectId(), authentication);
    }

    public boolean canAccessQuestionVersion(UUID versionId, Authentication authentication) {
        if (versionId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        Optional<QuestionVersion> versionOpt = questionVersionRepository.findById(versionId);
        if (versionOpt.isEmpty()) {
            return false;
        }

        return canAccessQuestion(versionOpt.get().getQuestionId(), authentication);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof User user) {
            return user.getUserId();
        }
        return null;
    }
}
