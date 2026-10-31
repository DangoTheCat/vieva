package com.example.vieva.application.ports.input.exam;

import java.time.Instant;

public interface ExpireExamAttemptsUseCase {

    /**
     * Times out IN_PROGRESS attempts whose server-side clock ran out (then dispatches grading)
     * and interrupts RECONNECTING attempts whose grace period passed.
     *
     * @return number of attempts changed
     */
    int expireOverdueAttempts(Instant now);
}
