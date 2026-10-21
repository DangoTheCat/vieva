package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.exam.ExamAttemptView;
import com.example.vieva.application.ports.input.exam.StartExamUseCase;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamTurnRepository;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.UserRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
import com.example.vieva.domain.entities.User;
import com.example.vieva.domain.entities.UserStatus;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class StartExamUseCaseImpl implements StartExamUseCase {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamTurnRepository examTurnRepository;

    @Override
    @UseCaseTransaction
    public ExamAttemptView startExam(UUID studentId, UUID subjectId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND, "Student not found: " + studentId));
        if (student.getStatus() != UserStatus.ACTIVE) {
            throw new ApplicationException(ErrorCode.USER_INACTIVE, "Student account is not active");
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.SUBJECT_NOT_FOUND, "Subject not found: " + subjectId));
        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw new ApplicationException(ErrorCode.SUBJECT_INACTIVE, "Subject is not active");
        }

        // Fetch question versions directly for this subject to select approved questions
        List<QuestionVersion> approvedVersions = questionVersionRepository.findByQuestionIds(
                questionRepository.findAllByIds(Collections.emptyList()).stream().map(Question::getQuestionId).toList()
        );

        // If searching by subject, we retrieve questions via subject search or version search
        // We can search approved question versions directly
        List<QuestionVersion> candidates;
        var searchCriteria = com.example.vieva.application.ports.common.QuestionBankSearchCriteria.builder()
                .subjectId(subjectId)
                .page(0)
                .size(50)
                .build();
        var pagedQuestions = questionRepository.searchBank(searchCriteria);
        List<Question> bankQuestions = pagedQuestions != null && pagedQuestions.getContent() != null
                ? pagedQuestions.getContent()
                : Collections.emptyList();

        if (bankQuestions.isEmpty()) {
            throw new ApplicationException(ErrorCode.QUESTION_NOT_FOUND, "No approved questions found in question bank for subject: " + subjectId);
        }

        // Select first approved question for the attempt
        Question firstQuestion = bankQuestions.get(0);
        UUID approvedVersionId = firstQuestion.getCurrentApprovedVersionId();
        if (approvedVersionId == null) {
            throw new ApplicationException(ErrorCode.NO_APPROVED_VERSION, "Question has no approved version: " + firstQuestion.getQuestionId());
        }

        QuestionVersion questionVersion = questionVersionRepository.findById(approvedVersionId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.QUESTION_VERSION_NOT_FOUND, "Approved question version not found: " + approvedVersionId));

        int totalQuestions = bankQuestions.size();

        ExamAttempt attempt = ExamAttempt.init(studentId, subjectId, totalQuestions);
        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);

        ExamTurn firstTurn = ExamTurn.createMainQuestion(
                savedAttempt.getId(),
                firstQuestion.getQuestionId(),
                questionVersion.getQuestionVersionId(),
                1,
                questionVersion.getQuestionContent(),
                null // TTS audio generated asynchronously or on-demand
        );
        ExamTurn savedTurn = examTurnRepository.save(firstTurn);

        return new ExamAttemptView(
                savedAttempt.getId(),
                savedAttempt.getStudentId(),
                savedAttempt.getSubjectId(),
                savedAttempt.getStatus().name(),
                savedAttempt.getCurrentQuestionIndex(),
                savedAttempt.getTotalQuestions(),
                savedAttempt.getTotalScore(),
                savedAttempt.getGradeLevel() != null ? savedAttempt.getGradeLevel().name() : null,
                savedAttempt.getStartedAt(),
                savedAttempt.getCompletedAt(),
                savedTurn.getId(),
                savedTurn.getQuestionText(),
                savedTurn.getTtsAudioUrl()
        );
    }
}
