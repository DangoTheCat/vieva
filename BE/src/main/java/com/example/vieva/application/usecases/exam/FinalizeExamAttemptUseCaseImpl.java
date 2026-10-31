package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.ports.input.exam.FinalizeExamAttemptUseCase;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamScoreRepository;
import com.example.vieva.application.ports.output.ExamTurnRepository;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamScore;
import com.example.vieva.domain.entities.ExamTurn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Business side of exam finalization, moved out of ExamFinalizationWorker (plan item 2.5 / audit A3):
 * the worker only schedules and retries, this use case owns the state transitions.
 */
@Slf4j
@RequiredArgsConstructor
public class FinalizeExamAttemptUseCaseImpl implements FinalizeExamAttemptUseCase {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamTurnRepository examTurnRepository;
    private final ExamScoreRepository examScoreRepository;
    private final AttemptGradingService attemptGradingService;

    @Override
    public Outcome finalizeAttempt(UUID attemptId) {
        Optional<ExamAttempt> attemptOpt = examAttemptRepository.findById(attemptId);
        if (attemptOpt.isEmpty()) {
            log.error("Attempt {} not found during finalization", attemptId);
            return Outcome.NOT_FOUND;
        }
        ExamAttempt attempt = attemptOpt.get();

        if (examScoreRepository.findByAttemptId(attemptId).isPresent()) {
            if (attempt.getStatus() == AttemptStatus.FINALIZING) {
                attempt.syncSuccess();
                examAttemptRepository.save(attempt);
            }
            log.info("Attempt {} already has a recorded score; status reconciled", attemptId);
            return Outcome.ALREADY_COMPLETED;
        }
        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            return Outcome.ALREADY_COMPLETED;
        }

        List<ExamTurn> turns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
        ExamScore score = attemptGradingService.gradeAndCompleteAttempt(attempt, turns);
        return score != null ? Outcome.COMPLETED : Outcome.NOT_GRADED;
    }

    @Override
    @UseCaseTransaction
    public void markFinalizationFailed(UUID attemptId) {
        examAttemptRepository.findById(attemptId).ifPresent(attempt -> {
            if (attempt.getStatus() == AttemptStatus.FINALIZING) {
                attempt.syncError();
                examAttemptRepository.save(attempt);
            }
        });
    }

    @Override
    @UseCaseTransaction(readOnly = true)
    public List<UUID> findAttemptsAwaitingFinalization() {
        return examAttemptRepository.findByStatus(AttemptStatus.FINALIZING).stream()
                .map(ExamAttempt::getId)
                .toList();
    }
}
