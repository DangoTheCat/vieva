package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.exam.ExamAttemptView;
import com.example.vieva.application.ports.input.exam.StartExamSessionUseCase;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamTurnRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamTurn;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class StartExamSessionUseCaseImpl implements StartExamSessionUseCase {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamTurnRepository examTurnRepository;

    @Override
    @UseCaseTransaction
    public ExamAttemptView startSession(UUID attemptId, UUID studentId, boolean deviceReady) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam attempt not found: " + attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "Student does not own this exam attempt");
        }

        attempt.start(deviceReady);
        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);

        List<ExamTurn> turns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
        ExamTurn currentTurn = turns.isEmpty() ? null : turns.get(turns.size() - 1);

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
                savedAttempt.getPausedAt(),
                savedAttempt.remainingAt(java.time.Instant.now()),
                savedAttempt.getGraceDeadline(),
                currentTurn != null ? currentTurn.getId() : null,
                currentTurn != null ? currentTurn.getQuestionText() : null,
                currentTurn != null ? currentTurn.getTtsAudioUrl() : null,
                savedAttempt.effectiveAnswerTimeLimitSeconds(),
                savedAttempt.effectiveMaxFollowUps()
        );
    }
}
