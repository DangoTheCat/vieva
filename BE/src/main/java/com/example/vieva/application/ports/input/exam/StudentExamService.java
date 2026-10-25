package com.example.vieva.application.ports.input.exam;

import java.util.List;
import java.util.UUID;

public interface StudentExamService {
    List<StudentExamView> listActiveExams(UUID studentId);
    ExamAttemptView enterExam(UUID examId, UUID studentId);
    List<StudentFrozenQuestionView> getAttemptQuestions(UUID attemptId, UUID studentId);
    List<ExamTurnDetailView> getAttemptTurns(UUID attemptId, UUID studentId);
    List<StudentScoreView> getStudentScores(UUID studentId);
}
