package com.example.vieva.application.ports.input.exam;

import java.util.List;
import java.util.UUID;

public record SetExamQuestionsCommand(
        List<ExamQuestionItem> questions
) {
    public record ExamQuestionItem(
            UUID questionId,
            UUID questionVersionId,
            int orderIndex
    ) {}
}
