package com.example.vieva.application.ports.input.exam;

import java.util.UUID;

/**
 * Starts the per-answer timer of a pending turn once its question has been read to the student (TTS done).
 */
public interface MarkTurnReadyUseCase {
    TurnReadyView markReady(UUID attemptId, UUID turnId, UUID studentId);
}
