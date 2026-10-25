package com.example.vieva.application.ports.input.exam;

import com.example.vieva.domain.entities.ExamStatus;

public record UpdateExamStatusCommand(
        ExamStatus status
) {}
