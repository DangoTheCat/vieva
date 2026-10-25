package com.example.vieva.application.ports.output;

import java.util.UUID;

public interface ExamFinalizationPort {
    void dispatchFinalization(UUID attemptId);
}
