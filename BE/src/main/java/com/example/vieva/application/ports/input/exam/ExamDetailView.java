package com.example.vieva.application.ports.input.exam;

import java.util.List;

public record ExamDetailView(
        ExamView exam,
        List<ExamQuestionView> questions
) {}
