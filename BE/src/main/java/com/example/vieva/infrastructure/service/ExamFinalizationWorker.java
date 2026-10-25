package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamFinalizationPort;
import com.example.vieva.application.ports.output.ExamScoreRepository;
import com.example.vieva.application.ports.output.ExamTurnRepository;
import com.example.vieva.application.usecases.exam.AttemptGradingService;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;
import com.example.vieva.domain.entities.ExamScore;
import com.example.vieva.domain.entities.ExamTurn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExamFinalizationWorker implements ExamFinalizationPort {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamTurnRepository examTurnRepository;
    private final ExamScoreRepository examScoreRepository;
    private final AttemptGradingService attemptGradingService;

    @Autowired(required = false)
    private TaskExecutor taskExecutor;

    private static final int MAX_RETRIES = 3;

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
        // Idempotent: check if attempt already has a saved score
        Optional<ExamScore> existingScore = examScoreRepository.findByAttemptId(attemptId);
        if (existingScore.isPresent()) {
            examAttemptRepository.findById(attemptId).ifPresent(att -> {
                if (att.getStatus() == AttemptStatus.FINALIZING) {
                    att.syncSuccess();
                    examAttemptRepository.save(att);
                }
            });
            log.info("Attempt {} already has recorded score, reconciled status if needed and skipping finalization", attemptId);
            return;
        }

        int attempts = 0;
        boolean success = false;
        while (attempts < MAX_RETRIES && !success) {
            attempts++;
            try {
                Optional<ExamAttempt> attemptOpt = examAttemptRepository.findById(attemptId);
                if (attemptOpt.isEmpty()) {
                    log.error("Attempt {} not found during finalization", attemptId);
                    return;
                }
                ExamAttempt attempt = attemptOpt.get();
                if (attempt.getStatus() == AttemptStatus.COMPLETED) {
                    log.info("Attempt {} is already COMPLETED", attemptId);
                    return;
                }

                List<ExamTurn> turns = examTurnRepository.findByAttemptIdOrderByTurnOrderAsc(attemptId);
                ExamScore score = attemptGradingService.gradeAndCompleteAttempt(attempt, turns);
                if (score != null) {
                    success = true;
                    log.info("Successfully graded and completed attempt {} on attempt {}", attemptId, attempts);
                }
            } catch (Exception ex) {
                log.warn("Finalization attempt {}/{} for attemptId {} failed: {}", attempts, MAX_RETRIES, attemptId, ex.getMessage());
                try {
                    Thread.sleep(300L * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (!success) {
            log.error("Failed to finalize attempt {} after {} retries. Setting status to INTERRUPTED", attemptId, MAX_RETRIES);
            try {
                Optional<ExamAttempt> attemptOpt = examAttemptRepository.findById(attemptId);
                if (attemptOpt.isPresent()) {
                    ExamAttempt attempt = attemptOpt.get();
                    if (attempt.getStatus() == AttemptStatus.FINALIZING) {
                        attempt.syncError();
                        examAttemptRepository.save(attempt);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to mark attempt {} as INTERRUPTED: {}", attemptId, e.getMessage());
            }
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void recoverStuckAttempts() {
        try {
            List<ExamAttempt> finalizing = examAttemptRepository.findByStatus(AttemptStatus.FINALIZING);
            for (ExamAttempt attempt : finalizing) {
                log.info("Recovering stuck FINALIZING attempt {}", attempt.getId());
                executeFinalization(attempt.getId());
            }
        } catch (Exception e) {
            log.warn("Error during scheduled finalization recovery: {}", e.getMessage());
        }
    }
}
