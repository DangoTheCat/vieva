package com.example.vieva.application.usecases.access;

import com.example.vieva.application.ports.input.access.CourseAccessQuery;
import com.example.vieva.application.ports.output.CourseDocumentRepository;
import com.example.vieva.application.ports.output.LecturerSubjectRepository;
import com.example.vieva.application.ports.output.QuestionGenerationRequestRepository;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.domain.entities.CourseDocument;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionGenerationRequest;
import com.example.vieva.domain.entities.QuestionVersion;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class CourseAccessQueryImpl implements CourseAccessQuery {

    private final LecturerSubjectRepository lecturerSubjectRepository;
    private final CourseDocumentRepository courseDocumentRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionGenerationRequestRepository generationRequestRepository;

    @Override
    public boolean canAccessSubject(UUID subjectId, UUID actorId, boolean isAdmin) {
        if (subjectId == null || actorId == null) {
            return false;
        }
        if (isAdmin) {
            return true;
        }
        return lecturerSubjectRepository.isLecturerAssignedToSubject(actorId, subjectId);
    }

    @Override
    public boolean canAccessDocument(UUID documentId, UUID actorId, boolean isAdmin) {
        if (documentId == null || actorId == null) {
            return false;
        }
        if (isAdmin) {
            return true;
        }
        return courseDocumentRepository.findById(documentId)
                .map(CourseDocument::getSubjectId)
                .map(subjectId -> canAccessSubject(subjectId, actorId, isAdmin))
                .orElse(true);
    }

    @Override
    public boolean canAccessQuestion(UUID questionId, UUID actorId, boolean isAdmin) {
        if (questionId == null || actorId == null) {
            return false;
        }
        if (isAdmin) {
            return true;
        }
        return questionRepository.findById(questionId)
                .map(Question::getSubjectId)
                .map(subjectId -> canAccessSubject(subjectId, actorId, isAdmin))
                .orElse(true);
    }

    @Override
    public boolean canAccessVersion(UUID versionId, UUID actorId, boolean isAdmin) {
        if (versionId == null || actorId == null) {
            return false;
        }
        if (isAdmin) {
            return true;
        }
        return questionVersionRepository.findById(versionId)
                .map(QuestionVersion::getQuestionId)
                .map(questionId -> canAccessQuestion(questionId, actorId, isAdmin))
                .orElse(true);
    }

    @Override
    public boolean canAccessGenerationRequest(UUID requestId, UUID actorId, boolean isAdmin) {
        if (requestId == null || actorId == null) {
            return false;
        }
        if (isAdmin) {
            return true;
        }
        return generationRequestRepository.findById(requestId)
                .map(QuestionGenerationRequest::getSubjectId)
                .map(subjectId -> canAccessSubject(subjectId, actorId, isAdmin))
                .orElse(true);
    }
}
