import { apiClient } from './client';

export const oralExamApi = {
  /**
   * Start a new oral exam attempt for a subject
   * POST /api/v1/student/attempts
   * @param {string|number} subjectId
   * @returns {Promise<any>}
   */
  startExam(subjectId) {
    return apiClient.post('/student/attempts', { subjectId });
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
