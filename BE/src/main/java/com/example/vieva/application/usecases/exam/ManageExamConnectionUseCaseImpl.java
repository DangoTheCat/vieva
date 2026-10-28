package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.exam.ExamAttemptView;
import com.example.vieva.application.ports.input.exam.ManageExamConnectionUseCase;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamFinalizationPort;
import com.example.vieva.application.ports.output.ExamTurnRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.exception.DomainException;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
public class ManageExamConnectionUseCaseImpl implements ManageExamConnectionUseCase {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamTurnRepository examTurnRepository;
    private final ExamFinalizationPort examFinalizationPort;

    public ManageExamConnectionUseCaseImpl(
            ExamAttemptRepository examAttemptRepository,
            ExamTurnRepository examTurnRepository,
            AttemptGradingService attemptGradingService,
            ExamFinalizationPort examFinalizationPort
    ) {
        this.examAttemptRepository = examAttemptRepository;
        this.examTurnRepository = examTurnRepository;
        this.examFinalizationPort = examFinalizationPort;
    }

    public ManageExamConnectionUseCaseImpl(
            ExamAttemptRepository examAttemptRepository,
            ExamTurnRepository examTurnRepository,
            ExamFinalizationPort examFinalizationPort
    ) {
        this(examAttemptRepository, examTurnRepository, null, examFinalizationPort);
    }

    public ManageExamConnectionUseCaseImpl(
            ExamAttemptRepository examAttemptRepository,
            ExamTurnRepository examTurnRepository,
            AttemptGradingService attemptGradingService
    ) {
        this(examAttemptRepository, examTurnRepository, attemptId -> {
            if (attemptGradingService != null) {
                examAttemptRepository.findById(attemptId).ifPresent(attempt -> {
                    List<ExamTurn> turns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
                    attemptGradingService.gradeAndCompleteAttempt(attempt, turns);
                });
            }
        });
    }

    @Override
    @UseCaseTransaction
    public ExamAttemptView disconnect(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = getOwnedAttemptForUpdate(attemptId, studentId);
        attempt.disconnect();
        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);
        return toView(savedAttempt, findCurrentTurn(attemptId));
    }

    @Override
    @UseCaseTransaction
    public ExamAttemptView reconnect(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = getOwnedAttemptForUpdate(attemptId, studentId);
        try {
            attempt.reconnect();
        } catch (DomainException ex) {
            if (attempt.getStatus() == AttemptStatus.INTERRUPTED) {
                examAttemptRepository.save(attempt);
            }
            throw ex;
        }
        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);
        return toView(savedAttempt, findCurrentTurn(attemptId));
    }

    @Override
    @UseCaseTransaction
    public ExamAttemptView finish(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = getOwnedAttemptForUpdate(attemptId, studentId);

        // Idempotency: repeated finish returns existing state without duplicate finalization
        if (attempt.getStatus() == AttemptStatus.FINALIZING || attempt.getStatus() == AttemptStatus.COMPLETED) {
            return toView(attempt, findCurrentTurn(attemptId));
        }

        attempt.finish();
        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);
        ExamTurn currentTurn = findCurrentTurn(attemptId);
        ExamAttemptView view = toView(savedAttempt, currentTurn);

        if (examFinalizationPort != null) {
            examFinalizationPort.dispatchFinalization(attemptId);
        }

        return view;
    }

    private ExamAttempt getOwnedAttemptForUpdate(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = examAttemptRepository.findByIdForUpdate(attemptId)
                .or(() -> examAttemptRepository.findById(attemptId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam attempt not found: " + attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "Student does not own this exam attempt");
        }
        return attempt;
    }

    private ExamTurn findCurrentTurn(UUID attemptId) {
        List<ExamTurn> turns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
        return turns.isEmpty() ? null : turns.get(turns.size() - 1);
    }

    private ExamAttemptView toView(ExamAttempt attempt, ExamTurn turn) {
        return new ExamAttemptView(
                attempt.getId(),
                attempt.getStudentId(),
                attempt.getSubjectId(),
                attempt.getStatus().name(),
                attempt.getCurrentQuestionIndex(),
                attempt.getTotalQuestions(),
                attempt.getTotalScore(),
                attempt.getGradeLevel() != null ? attempt.getGradeLevel().name() : null,
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                attempt.getPausedAt(),
                attempt.remainingSecondsAt(Instant.now()),
                attempt.getGraceDeadline(),
                turn != null ? turn.getId() : null,
                turn != null ? turn.getQuestionText() : null,
                turn != null ? turn.getTtsAudioUrl() : null
        );
    }
}
