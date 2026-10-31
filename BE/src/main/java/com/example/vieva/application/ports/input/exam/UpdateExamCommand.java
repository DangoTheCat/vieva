package com.example.vieva.application.ports.input.exam;

public record UpdateExamCommand(
        String title,
        Integer answerTimeLimitSeconds,
        Integer maxFollowUpsPerQuestion
) {
    public UpdateExamCommand(String title) {
        this(title, null, null);
    }

    public boolean isEmpty() {
        return title == null && answerTimeLimitSeconds == null && maxFollowUpsPerQuestion == null;
    }
}
