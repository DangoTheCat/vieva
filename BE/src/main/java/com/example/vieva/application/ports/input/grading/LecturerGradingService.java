package com.example.vieva.application.ports.input.grading;

import com.example.vieva.application.ports.input.exam.ExamView;

import java.util.List;
import java.util.UUID;

public interface LecturerGradingService {
    List<GradingCourseView> getCoursesForGrading(UUID lecturerId);
    List<ExamView> getExamsForCourse(UUID courseId, UUID lecturerId);
    List<ExamParticipantView> getExamParticipants(UUID examId, UUID lecturerId);
    AttemptGradingDetailView getAttemptDetail(UUID attemptId, UUID lecturerId);
    AttemptGradingDetailView gradeQuestion(UUID attemptId, UUID questionId, GradeQuestionCommand command, UUID lecturerId);
    List<LecturerScoreView> listScores(UUID lecturerId);
    LecturerScoreView overrideScore(UUID scoreId, OverrideScoreCommand command, UUID lecturerId);
    ExamGradingStatsView getExamStats(UUID examId, UUID lecturerId);
    CourseGradingStatsView getCourseStats(UUID courseId, UUID lecturerId);
}
