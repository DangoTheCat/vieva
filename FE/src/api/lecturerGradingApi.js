import { apiClient } from './client';

/** Grading & score review for the lecturer's assigned subjects ("courses" on the BE). */
export const lecturerGradingApi = {
  /** GET /api/v1/lecturer/grading/courses — assigned subjects with grading counters */
  getCourses() {
    return apiClient.get('/lecturer/grading/courses');
  },

  /** GET /api/v1/lecturer/courses/{courseId}/exams */
  getCourseExams(courseId) {
    return apiClient.get(`/lecturer/courses/${courseId}/exams`);
  },

  /** GET /api/v1/lecturer/courses/{courseId}/stats */
  getCourseStats(courseId) {
    return apiClient.get(`/lecturer/courses/${courseId}/stats`);
  },

  /** GET /api/v1/lecturer/exams/{examId}/participants */
  getParticipants(examId) {
    return apiClient.get(`/lecturer/exams/${examId}/participants`);
  },

  /** GET /api/v1/lecturer/exams/{examId}/stats */
  getExamStats(examId) {
    return apiClient.get(`/lecturer/exams/${examId}/stats`);
  },

  /** GET /api/v1/lecturer/attempts/{attemptId} — per-question turns, AI and teacher scores */
  getAttempt(attemptId) {
    return apiClient.get(`/lecturer/attempts/${attemptId}`);
  },

  /** PUT /api/v1/lecturer/attempts/{attemptId}/questions/{questionId} — score 0..10 */
  gradeQuestion(attemptId, questionId, { score, feedback }) {
    return apiClient.put(`/lecturer/attempts/${attemptId}/questions/${questionId}`, { score, feedback });
  },

  /** GET /api/v1/lecturer/scores */
  listScores() {
    return apiClient.get('/lecturer/scores');
  },

  /** PATCH /api/v1/lecturer/scores/{scoreId} — override the total (0..10) with a reason */
  overrideScore(scoreId, { overrideTotalScore, reason }) {
    return apiClient.patch(`/lecturer/scores/${scoreId}`, { overrideTotalScore, reason });
  }
};
