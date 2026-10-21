package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.exam.TurnEvaluationView;
import com.example.vieva.application.ports.input.exam.SubmitTurnAnswerUseCase;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamTurnRepository;
import com.example.vieva.application.ports.output.FileStoragePort;
import com.example.vieva.application.ports.output.QuestionRepository;
import com.example.vieva.application.ports.output.QuestionVersionRepository;
import com.example.vieva.application.ports.output.RubricRepository;
import com.example.vieva.application.ports.output.StoredFile;
import com.example.vieva.application.ports.output.VivaEvaluationGateway;
import com.example.vieva.application.ports.output.VivaEvaluationResult;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.entities.Question;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Rubric;
import com.example.vieva.domain.entities.TurnType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SubmitTurnAnswerUseCaseImpl implements SubmitTurnAnswerUseCase {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamTurnRepository examTurnRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final RubricRepository rubricRepository;
    private final FileStoragePort fileStoragePort;
    private final VivaEvaluationGateway vivaEvaluationGateway;
    private final AttemptGradingService attemptGradingService;

    @Override
    @UseCaseTransaction
    public TurnEvaluationView submitAnswer(
            UUID attemptId,
            UUID turnId,
            UUID studentId,
            String transcriptText,
            byte[] audioBytes,
            String audioContentType
    ) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam attempt not found: " + attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "Student does not own this exam attempt");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST,
                    "Exam attempt is not IN_PROGRESS (current: " + attempt.getStatus() + ")");
        }

        ExamTurn currentTurn = examTurnRepository.findById(turnId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Turn not found: " + turnId));

        if (!currentTurn.getAttemptId().equals(attemptId)) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Turn does not belong to this attempt");
        }

        // Upload audio if provided
        String studentAudioUrl = null;
        if (audioBytes != null && audioBytes.length > 0) {
            String storagePath = String.format("oral-exams/%s/%s.wav", attemptId, turnId);
            String contentType = audioContentType != null && !audioContentType.isBlank() ? audioContentType : "audio/wav";
            StoredFile storedFile = fileStoragePort.store(storagePath, audioBytes, contentType);
            studentAudioUrl = storedFile != null ? storedFile.url() : null;
        }

        // Update current turn answer
        currentTurn.submitAnswer(transcriptText, studentAudioUrl);

        // Fetch rubric for question version
        Rubric rubric = currentTurn.getQuestionVersionId() != null
                ? rubricRepository.findByQuestionVersionId(currentTurn.getQuestionVersionId()).orElse(null)
                : null;

        // Evaluate turn
        VivaEvaluationResult evalResult = vivaEvaluationGateway.evaluateTurn(currentTurn, rubric);
        currentTurn.evaluate(
                evalResult.score(),
                evalResult.feedback(),
                evalResult.rubricEvaluations()
        );
        examTurnRepository.save(currentTurn);

        // Check follow-up budget rule:
        // Find all turns for this attempt and current questionId
        List<ExamTurn> questionTurns = examTurnRepository.findByAttemptIdAndQuestionIdOrderByTurnOrderAsc(
                attemptId, currentTurn.getQuestionId()
        );

        long followUpCount = questionTurns.stream()
                .filter(t -> t.getTurnType() == TurnType.FOLLOW_UP)
                .count();

        boolean canFollowUp = evalResult.shouldFollowUp()
                && !evalResult.isStandardMet()
                && followUpCount < 2
                && evalResult.followUpQuestionText() != null
                && !evalResult.followUpQuestionText().isBlank();

        UUID nextTurnId = null;
        String nextQuestionText = null;
        String nextTurnType = null;
        boolean isAttemptFinished = false;

        List<ExamTurn> allAttemptTurns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
        int nextTurnOrder = allAttemptTurns.size() + 1;

        if (canFollowUp) {
            ExamTurn followUpTurn = ExamTurn.createFollowUp(
                    attemptId,
                    currentTurn.getQuestionId(),
                    currentTurn.getQuestionVersionId(),
                    currentTurn.getId(),
                    nextTurnOrder,
                    evalResult.followUpQuestionText(),
                    null
            );
            ExamTurn savedFollowUp = examTurnRepository.save(followUpTurn);
            nextTurnId = savedFollowUp.getId();
            nextQuestionText = savedFollowUp.getQuestionText();
            nextTurnType = TurnType.FOLLOW_UP.name();
        } else {
            // Advance to next main question or complete attempt
            var searchCriteria = com.example.vieva.application.ports.common.QuestionBankSearchCriteria.builder()
                    .subjectId(attempt.getSubjectId())
                    .page(0)
                    .size(50)
                    .build();
            var pagedQuestions = questionRepository.searchBank(searchCriteria);
            List<Question> bankQuestions = pagedQuestions != null && pagedQuestions.getContent() != null
                    ? pagedQuestions.getContent()
                    : Collections.emptyList();

            int nextIndex = attempt.getCurrentQuestionIndex() + 1;
            if (nextIndex < bankQuestions.size()) {
                attempt.nextQuestion();
                examAttemptRepository.save(attempt);

                Question nextQuestion = bankQuestions.get(nextIndex);
                UUID nextApprovedVersionId = nextQuestion.getCurrentApprovedVersionId();
                QuestionVersion nextVersion = nextApprovedVersionId != null
                        ? questionVersionRepository.findById(nextApprovedVersionId).orElse(null)
                        : null;

                String qContent = nextVersion != null ? nextVersion.getQuestionContent() : "";
                UUID vId = nextVersion != null ? nextVersion.getQuestionVersionId() : null;

                ExamTurn nextMainTurn = ExamTurn.createMainQuestion(
                        attemptId,
                        nextQuestion.getQuestionId(),
                        vId,
                        nextTurnOrder,
                        qContent,
                        null
                );
                ExamTurn savedNextMainTurn = examTurnRepository.save(nextMainTurn);
                nextTurnId = savedNextMainTurn.getId();
                nextQuestionText = savedNextMainTurn.getQuestionText();
                nextTurnType = TurnType.MAIN_QUESTION.name();
            } else {
                // All questions finished, trigger grading and complete attempt
                isAttemptFinished = true;
                List<ExamTurn> finalizedTurns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
                attemptGradingService.gradeAndCompleteAttempt(attempt, finalizedTurns);
            }
        }

        return new TurnEvaluationView(
                currentTurn.getId(),
                currentTurn.getAttemptId(),
                currentTurn.getTurnType().name(),
                currentTurn.getTurnOrder(),
                currentTurn.getStatus().name(),
                currentTurn.getScore(),
                currentTurn.getFeedback(),
                currentTurn.getRubricEvaluations(),
                currentTurn.getStudentAudioUrl(),
                currentTurn.getTranscriptText(),
                isAttemptFinished,
                nextTurnId,
                nextQuestionText,
                nextTurnType,
                currentTurn.getUpdatedAt()
        );
    }
}
