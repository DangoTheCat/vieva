package com.example.vieva.application.usecases.question;

import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Loads a question / version and re-checks the actor's subject assignment (BR-06) in the caller's
 * transaction.
 */
@Component
@RequiredArgsConstructor
public class QuestionAccessLoader {

    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final SubjectAccessGuard subjectAccessGuard;

    public record VersionContext(Question question, QuestionVersion version) {
    }

    public VersionContext versionForWrite(UUID versionId, UUID actorId) {
        QuestionVersion version = questionVersionRepository.findById(versionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_VERSION_NOT_FOUND));
        Question question = questionRepository.findById(version.getQuestionId())
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        subjectAccessGuard.requireManage(actorId, question.getSubjectId());
        return new VersionContext(question, version);
    }

    public Question questionForWrite(UUID questionId, UUID actorId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        subjectAccessGuard.requireManage(actorId, question.getSubjectId());
        return question;
    }
}
