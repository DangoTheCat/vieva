package com.example.vieva.application.ports.output;

import java.util.UUID;

public interface ExamSttContextPort {
    void dispatch(UUID examId);
}
