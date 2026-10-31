import { apiClient } from './client';

export const oralExamApi = {
  /**
   * Exams currently open (IN_PROGRESS) with this student's attempt status, if any
   * GET /api/v1/student/exams
   * @returns {Promise<Array<{id: string, subjectId: string, title: string, durationSeconds: number, questionCount: number, studentAttemptStatus: string|null, studentAttemptId: string|null}>>}
   */
  listExams() {
    return apiClient.get('/student/exams');
  },

  /**
   * Enter an open exam; returns the existing attempt when the student already entered
   * POST /api/v1/student/exams/{examId}/enter
   * @param {string} examId
   * @returns {Promise<any>}
   */
  enterExam(examId) {
    return apiClient.post(`/student/exams/${examId}/enter`);
  },

  /**
   * Completed attempts with their scores
   * GET /api/v1/student/scores
   * @returns {Promise<Array<{attemptId: string, examId: string, examTitle: string, totalScore: number, gradeLevel: string, completedAt: string}>>}
   */
  getScores() {
    return apiClient.get('/student/scores');
  },

  /**
   * Questions frozen into the attempt when it was created
   * GET /api/v1/student/attempts/{attemptId}/questions
   * @param {string} attemptId
   * @returns {Promise<Array<{questionId: string, questionVersionId: string, orderIndex: number, questionText: string}>>}
   */
  getAttemptQuestions(attemptId) {
    return apiClient.get(`/student/attempts/${attemptId}/questions`);
  },

  /**
   * Turns (main questions and probes) asked so far in the attempt
   * GET /api/v1/student/attempts/{attemptId}/turns
   * @param {string} attemptId
   * @returns {Promise<any[]>}
   */
  getAttemptTurns(attemptId) {
    return apiClient.get(`/student/attempts/${attemptId}/turns`);
  },

  /**
   * Get attempt detail including current question, turns, and status
   * GET /api/v1/student/attempts/{attemptId}
   * @param {string|number} attemptId
   * @returns {Promise<any>}
   */
  getAttemptDetail(attemptId) {
    return apiClient.get(`/student/attempts/${attemptId}`);
  },

  /**
   * Transition exam from READY to IN_PROGRESS after pre-flight check
   * POST /api/v1/student/attempts/{attemptId}/start
   * @param {string|number} attemptId
   * @param {boolean} [deviceReady=true]
   * @returns {Promise<any>}
   */
  startExamSession(attemptId, deviceReady = true) {
    return apiClient.post(`/student/attempts/${attemptId}/start`, { deviceReady });
  },

  /**
   * The question has been read aloud: start the answer timer on the server
   * POST /api/v1/student/attempts/{attemptId}/turns/{turnId}/ready
   * @returns {Promise<{turnId: string, askedAt: string, answerDeadline: string, answerTimeLimitSeconds: number, remainingSeconds: number, serverTime: string}>}
   */
  markTurnReady(attemptId, turnId) {
    return apiClient.post(`/student/attempts/${attemptId}/turns/${turnId}/ready`);
  },

  /**
   * Mark exam as disconnected and pause timer
   * POST /api/v1/student/attempts/{attemptId}/disconnect
   * @param {string|number} attemptId
   * @returns {Promise<any>}
   */
  disconnectExam(attemptId) {
    return apiClient.post(`/student/attempts/${attemptId}/disconnect`);
  },

  /**
   * Reconnect to exam and resume timer
   * POST /api/v1/student/attempts/{attemptId}/reconnect
   * @param {string|number} attemptId
   * @returns {Promise<any>}
   */
  reconnectExam(attemptId) {
    return apiClient.post(`/student/attempts/${attemptId}/reconnect`);
  },

  /**
   * Finish exam session and transition to FINALIZING
   * POST /api/v1/student/attempts/{attemptId}/finish
   * @param {string|number} attemptId
   * @returns {Promise<any>}
   */
  finishExam(attemptId) {
    return apiClient.post(`/student/attempts/${attemptId}/finish`);
  },

  /**
   * Submit an answer for a specific question turn
   * POST /api/v1/student/attempts/{attemptId}/turns/{turnId}/submit
   * @param {string|number} attemptId
   * @param {string|number} turnId
   * @param {{ transcriptText?: string, audioBlob?: Blob }} payload
   * @returns {Promise<any>}
   */
  submitTurnAnswer(attemptId, turnId, { transcriptText, audioBlob } = {}) {
    const formData = new FormData();
    if (transcriptText) {
      formData.append('transcriptText', transcriptText);
    }
    if (audioBlob) {
      formData.append('audioFile', audioBlob, 'turn_answer.webm');
    }
    return apiClient.post(`/student/attempts/${attemptId}/turns/${turnId}/submit`, formData);
  }
};
