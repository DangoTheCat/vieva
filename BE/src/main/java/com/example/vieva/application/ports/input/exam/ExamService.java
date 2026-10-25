package com.example.vieva.application.ports.input.exam;

import java.util.List;
import java.util.UUID;

public interface ExamService {
    ExamView createExam(CreateExamCommand command, UUID actorId);
    List<ExamView> listExamsForSubject(UUID subjectId, UUID actorId);
    ExamDetailView getExam(UUID examId, UUID actorId);
    ExamView updateExamTitle(UUID examId, UpdateExamCommand command, UUID actorId);
    ExamView updateExamStatus(UUID examId, UpdateExamStatusCommand command, UUID actorId);
    void deleteExam(UUID examId, UUID actorId);
    ExamDetailView setExamQuestions(UUID examId, SetExamQuestionsCommand command, UUID actorId);
}
