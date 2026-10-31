package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.input.exam.FinalizeExamAttemptUseCase;
import com.example.vieva.application.ports.input.exam.FinalizeExamAttemptUseCase.Outcome;
import com.example.vieva.application.ports.output.ExamFinalizationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Humble delivery mechanism for exam finalization: runs it after commit, asynchronously, with
 * retries and a periodic sweep. All state changes happen in {@link FinalizeExamAttemptUseCase}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamFinalizationWorker implements ExamFinalizationPort {

    private static final int MAX_RETRIES = 3;

    private final FinalizeExamAttemptUseCase finalizeExamAttemptUseCase;

    @Autowired(required = false)
    private TaskExecutor taskExecutor;

    @Override
    public void dispatchFinalization(UUID attemptId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runAsyncFinalization(attemptId);
                }
            });
        } else {
            runAsyncFinalization(attemptId);
        }
    }

    private void runAsyncFinalization(UUID attemptId) {
        if (taskExecutor != null) {
            taskExecutor.execute(() -> executeFinalization(attemptId));
        } else {
            CompletableFuture.runAsync(() -> executeFinalization(attemptId));
        }
    }

    public void executeFinalization(UUID attemptId) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Outcome outcome = finalizeExamAttemptUseCase.finalizeAttempt(attemptId);
                if (outcome != Outcome.NOT_GRADED) {
                    log.info("Finalization of attempt {} finished with {} (try {})", attemptId, outcome, attempt);
                    return;
                }
            } catch (Exception ex) {
                log.warn("Finalization try {}/{} for attempt {} failed: {}", attempt, MAX_RETRIES, attemptId, ex.getMessage());
            }
            try {
                Thread.sleep(300L * attempt);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.error("Failed to finalize attempt {} after {} tries; marking it INTERRUPTED", attemptId, MAX_RETRIES);
        try {
            finalizeExamAttemptUseCase.markFinalizationFailed(attemptId);
        } catch (Exception e) {
            log.error("Failed to mark attempt {} as INTERRUPTED: {}", attemptId, e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void recoverStuckAttempts() {
        try {
            for (UUID attemptId : finalizeExamAttemptUseCase.findAttemptsAwaitingFinalization()) {
                log.info("Recovering stuck FINALIZING attempt {}", attemptId);
                executeFinalization(attemptId);
            }
        } catch (Exception e) {
            log.warn("Error during scheduled finalization recovery: {}", e.getMessage());
        }
    }
}
