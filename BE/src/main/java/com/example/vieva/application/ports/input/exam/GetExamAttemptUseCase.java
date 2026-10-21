package com.example.vieva.application.ports.input.exam;

import java.util.UUID;

public interface GetExamAttemptUseCase {
    ExamAttemptDetailView getAttemptDetail(UUID attemptId, UUID studentId);
}
