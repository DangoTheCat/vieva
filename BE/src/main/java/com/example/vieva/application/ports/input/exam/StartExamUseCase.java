package com.example.vieva.application.ports.input.exam;

import java.util.UUID;

public interface StartExamUseCase {
    ExamAttemptView startExam(UUID studentId, UUID subjectId);
}
