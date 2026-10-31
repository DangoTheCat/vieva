import { apiClient } from './client';

/**
 * Exam lifecycle for lecturers: DRAFT -> IN_PROGRESS (open to students) -> COMPLETED.
 * Questions can only be replaced, and the exam deleted, while it is DRAFT with no attempts.
 */
export const lecturerExamApi = {
  /** GET /api/v1/lecturer/exams?subjectId= */
  listExams(subjectId) {
    return apiClient.get('/lecturer/exams', { subjectId });
  },

  /** GET /api/v1/lecturer/exams/{id} — includes the ordered question list */
  getExam(examId) {
    return apiClient.get(`/lecturer/exams/${examId}`);
  },

  /** POST /api/v1/lecturer/exams */
  createExam({ subjectId, title, durationSeconds, answerTimeLimitSeconds, maxFollowUpsPerQuestion }) {
    return apiClient.post('/lecturer/exams', {
      subjectId, title, durationSeconds, answerTimeLimitSeconds, maxFollowUpsPerQuestion
    });
  },

  /** PATCH /api/v1/lecturer/exams/{id} */
  updateTitle(examId, title) {
    return apiClient.patch(`/lecturer/exams/${examId}`, { title });
  },

  /**
   * PATCH /api/v1/lecturer/exams/{id} — DRAFT only; omitted fields keep their value
   * @param {{ title?: string, answerTimeLimitSeconds?: number, maxFollowUpsPerQuestion?: number }} changes
   */
  updateExam(examId, changes) {
    return apiClient.patch(`/lecturer/exams/${examId}`, changes);
  },

  /** PATCH /api/v1/lecturer/exams/{id}/status — status: 'IN_PROGRESS' | 'COMPLETED' */
  updateStatus(examId, status) {
    return apiClient.patch(`/lecturer/exams/${examId}/status`, { status });
  },

  /** DELETE /api/v1/lecturer/exams/{id} */
  deleteExam(examId) {
    return apiClient.delete(`/lecturer/exams/${examId}`);
  },

  /**
   * PUT /api/v1/lecturer/exams/{id}/questions — replaces the whole list; versions must be APPROVED
   * @param {Array<{questionId: string, questionVersionId: string, orderIndex: number}>} questions
   */
  setQuestions(examId, questions) {
    return apiClient.put(`/lecturer/exams/${examId}/questions`, { questions });
  }
};
