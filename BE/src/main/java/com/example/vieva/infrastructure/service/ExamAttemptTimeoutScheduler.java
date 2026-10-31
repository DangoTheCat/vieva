package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.input.exam.ExpireExamAttemptsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExamAttemptTimeoutScheduler {

    private final ExpireExamAttemptsUseCase expireExamAttemptsUseCase;

    @Scheduled(fixedDelayString = "${vieva.schedule.exam-timeout-delay-ms:30000}", initialDelay = 10000)
    public void expireOverdueAttempts() {
        int changed = expireExamAttemptsUseCase.expireOverdueAttempts(Instant.now());
        if (changed > 0) {
            log.info("Timed out {} overdue exam attempts", changed);
        }
    }
}
