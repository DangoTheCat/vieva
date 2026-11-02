package com.example.vieva.application.ports.input.speech;

import java.util.UUID;

public interface ExamSttContextQuery {
    String forAttempt(UUID attemptId, UUID studentId);
}
