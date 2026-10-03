import { apiClient } from './client';

export const questionBankApi = {
  // Official bank
  searchBank(subjectId, params = {}) {
    return apiClient.get(`/lecturer/subjects/${subjectId}/questions`, params);
  },

  createManualQuestion(subjectId, payload) {
    return apiClient.post(`/lecturer/subjects/${subjectId}/questions`, payload);
  },

  getQuestionDetail(questionId) {
    return apiClient.get(`/lecturer/questions/${questionId}`);
  },

  getHistory(questionId) {
    return apiClient.get(`/lecturer/questions/${questionId}/versions`);
  },

  createDraftFromApproved(questionId) {
    return apiClient.post(`/lecturer/questions/${questionId}/versions`);
  },

  archiveQuestion(questionId) {
    return apiClient.post(`/lecturer/questions/${questionId}/archive`);
  },

  // Review Queue / Versions
  searchVersions(subjectId, params = {}) {
    return apiClient.get('/lecturer/question-versions', { subjectId, ...params });
  },

  getVersion(versionId) {
    return apiClient.get(`/lecturer/question-versions/${versionId}`);
  },

  updateDraft(versionId, payload) {
    return apiClient.put(`/lecturer/question-versions/${versionId}`, payload);
  },

  confirmBloom(versionId, payload) {
    return apiClient.post(`/lecturer/question-versions/${versionId}/confirm-bloom`, payload);
  },

  approveVersion(versionId, payload = {}) {
    return apiClient.post(`/lecturer/question-versions/${versionId}/approve`, payload);
  },

  rejectVersion(versionId, payload) {
    return apiClient.post(`/lecturer/question-versions/${versionId}/reject`, payload);
  },

  deleteDraft(versionId) {
    return apiClient.delete(`/lecturer/question-versions/${versionId}`);
  },

  regenerateQuestion(versionId, payload = {}) {
    return apiClient.post(`/lecturer/question-versions/${versionId}/regenerate`, payload);
  }
};
