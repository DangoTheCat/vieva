package com.example.vieva.application.ports.input.exam;

import java.time.Instant;
import java.util.UUID;

/**
 * Answer timer of a turn after the question has been read aloud.
 *
 * @param answerDeadline         askedAt + answerTimeLimitSeconds (a short submit grace applies on top)
 * @param remainingSeconds       seconds left on the whole exam clock, computed by the server
 */
public record TurnReadyView(
        UUID turnId,
        UUID attemptId,
        Instant askedAt,
        Instant answerDeadline,
        int answerTimeLimitSeconds,
        int remainingSeconds,
        Instant serverTime
) {
}
