package com.example.vieva.application.ports.input.exam;

import java.util.UUID;

public interface StartExamSessionUseCase {
    ExamAttemptView startSession(UUID attemptId, UUID studentId, boolean deviceReady);
}
