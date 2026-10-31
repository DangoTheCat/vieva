package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.exception.ApplicationException;
import com.example.vieva.application.exception.ErrorCode;
import com.example.vieva.application.ports.input.exam.MarkTurnReadyUseCase;
import com.example.vieva.application.ports.input.exam.TurnReadyView;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamTurnRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamTurn;
import com.example.vieva.domain.entities.TurnStatus;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Starts the answer timer when the question has finished being read aloud.
 * Idempotent for an answered turn: it reports the recorded timing instead of failing.
 */
@RequiredArgsConstructor
public class MarkTurnReadyUseCaseImpl implements MarkTurnReadyUseCase {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamTurnRepository examTurnRepository;

    @Override
    @UseCaseTransaction
    public TurnReadyView markReady(UUID attemptId, UUID turnId, UUID studentId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Exam attempt not found: " + attemptId));
        if (!attempt.getStudentId().equals(studentId)) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "Student does not own this exam attempt");
        }
        ExamTurn turn = examTurnRepository.findById(turnId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Turn not found: " + turnId));
        if (!turn.getAttemptId().equals(attemptId)) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST, "Turn does not belong to this attempt");
        }

        Instant now = Instant.now();
        int limit = attempt.effectiveAnswerTimeLimitSeconds();
        if (turn.getStatus() == TurnStatus.PENDING) {
            attempt.ensureInProgress();
            turn.markAsked(now);
            turn = examTurnRepository.save(turn);
        }

        return new TurnReadyView(
                turn.getId(),
                attemptId,
                turn.getAskedAt(),
                turn.answerDeadline(limit),
                limit,
                attempt.remainingAt(now),
                now
        );
    }
}
