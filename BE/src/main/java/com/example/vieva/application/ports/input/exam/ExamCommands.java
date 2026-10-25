package com.example.vieva.application.ports.input.exam;

import com.example.vieva.domain.entities.ExamStatus;

import java.util.List;
import java.util.UUID;

public class ExamCommands {

    public record CreateExamCommand(
            UUID subjectId,
            String title,
            Integer durationSeconds
    ) {}

    public record UpdateExamTitleCommand(
            String title
    ) {}

    public record UpdateExamStatusCommand(
            ExamStatus status
    ) {}

    public record SetExamQuestionsCommand(
            List<UUID> questionVersionIds
    ) {}
}
