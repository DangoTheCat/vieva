package com.example.vieva.application.usecases.exam;

import com.example.vieva.application.ports.input.exam.ExpireExamAttemptsUseCase;
import com.example.vieva.application.ports.output.ExamAttemptRepository;
import com.example.vieva.application.ports.output.ExamFinalizationPort;
import com.example.vieva.application.transaction.UseCaseTransaction;
import com.example.vieva.domain.entities.AttemptStatus;
import com.example.vieva.domain.entities.ExamAttempt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class ExpireExamAttemptsUseCaseImpl implements ExpireExamAttemptsUseCase {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamFinalizationPort examFinalizationPort;

    @Override
    @UseCaseTransaction
    public int expireOverdueAttempts(Instant now) {
        int changed = 0;

        for (ExamAttempt candidate : examAttemptRepository.findByStatus(AttemptStatus.IN_PROGRESS)) {
            if (!candidate.isTimeUp(now)) {
                continue;
            }
            // Re-read under lock: the student may have submitted or disconnected meanwhile
            ExamAttempt attempt = examAttemptRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (attempt == null || !attempt.isTimeUp(now)) {
                continue;
            }
            attempt.timeoutSession();
            examAttemptRepository.save(attempt);
            examFinalizationPort.dispatchFinalization(attempt.getId());
            changed++;
        }

        for (ExamAttempt candidate : examAttemptRepository.findByStatus(AttemptStatus.RECONNECTING)) {
            if (!candidate.isGraceExpired(now)) {
                continue;
            }
            ExamAttempt attempt = examAttemptRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (attempt == null || !attempt.isGraceExpired(now)) {
                continue;
            }
            attempt.timeoutGrace();
            examAttemptRepository.save(attempt);
            changed++;
        }

        return changed;
    }
}
