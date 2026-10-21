package com.example.vieva.application.ports.input.exam;

import java.util.UUID;

public interface ManageExamConnectionUseCase {
    ExamAttemptView disconnect(UUID attemptId, UUID studentId);
    ExamAttemptView reconnect(UUID attemptId, UUID studentId);
    ExamAttemptView finish(UUID attemptId, UUID studentId);
}
