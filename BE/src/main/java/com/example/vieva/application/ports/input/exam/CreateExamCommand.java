package com.example.vieva.application.ports.input.exam;

import java.util.UUID;

public record CreateExamCommand(
        UUID subjectId,
        String title,
        Integer durationSeconds
) {}
