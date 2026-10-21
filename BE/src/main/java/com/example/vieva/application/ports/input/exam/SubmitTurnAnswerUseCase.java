package com.example.vieva.application.ports.input.exam;

import java.util.UUID;

public interface SubmitTurnAnswerUseCase {
    TurnEvaluationView submitAnswer(UUID attemptId, UUID turnId, UUID studentId, String transcriptText, byte[] audioBytes, String audioContentType);
}
