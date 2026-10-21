package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.exam.ExamAttemptView;
import com.example.vieva.application.ports.input.exam.ManageExamConnectionUseCase;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamTurnRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamTurn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public class ManageExamConnectionUseCaseImpl implements ManageExamConnectionUseCase {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamTurnRepository examTurnRepository;
    private final AttemptGradingService attemptGradingService;

    @Override
    @UseCaseTransaction
    public ExamAttemptView disconnect(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = getOwnedAttempt(attemptId, studentId);
        attempt.disconnect();
        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);
        return toView(savedAttempt, findCurrentTurn(attemptId));
    }

    @Override
    @UseCaseTransaction
    public ExamAttemptView reconnect(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = getOwnedAttempt(attemptId, studentId);
        attempt.reconnect();
        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);
        return toView(savedAttempt, findCurrentTurn(attemptId));
    }

    @Override
    @UseCaseTransaction
    public ExamAttemptView finish(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = getOwnedAttempt(attemptId, studentId);
        attempt.finish();
        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);
        ExamTurn currentTurn = findCurrentTurn(attemptId);
        ExamAttemptView view = toView(savedAttempt, currentTurn);

        if (attemptGradingService != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    List<ExamTurn> turns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
                    attemptGradingService.gradeAndCompleteAttempt(savedAttempt, turns);
                } catch (Exception ex) {
                    log.error("Async grading failed for attempt {}: {}", attemptId, ex.getMessage());
                }
            });
        }

        return view;
    }

    private ExamAttempt getOwnedAttempt(UUID attemptId, UUID studentId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
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
                turn != null ? turn.getId() : null,
                turn != null ? turn.getQuestionText() : null,
                turn != null ? turn.getTtsAudioUrl() : null
        );
    }
}
