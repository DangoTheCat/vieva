package com.example.vieva.application.ports.input.exam;

import java.util.List;
import java.util.UUID;

/**
 * Grades a FINALIZING attempt and moves it to COMPLETED (or INTERRUPTED when grading keeps failing).
 * Delivery (async execution, retries, scheduling) belongs to the caller in the infrastructure layer.
 */
public interface FinalizeExamAttemptUseCase {

    /** Outcome of one finalization try. */
    enum Outcome { COMPLETED, ALREADY_COMPLETED, NOT_FOUND, NOT_GRADED }

    /**
     * Grades the attempt once. Idempotent: an attempt that already has a score is only reconciled.
     * Throws when grading fails so the caller can retry.
     */
    Outcome finalizeAttempt(UUID attemptId);

    /** Gives up on grading: a FINALIZING attempt becomes INTERRUPTED. */
    void markFinalizationFailed(UUID attemptId);

    /** Attempts left in FINALIZING (e.g. after a restart) that still need grading. */
    List<UUID> findAttemptsAwaitingFinalization();
}
