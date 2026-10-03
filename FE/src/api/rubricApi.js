import { apiClient } from './client';

export const rubricApi = {
  getRubric(versionId) {
    return apiClient.get(`/lecturer/question-versions/${versionId}/rubric`);
  },

  replaceRubric(versionId, payload) {
    return apiClient.put(`/lecturer/question-versions/${versionId}/rubric`, payload);
  },

  addCriterion(versionId, payload) {
    return apiClient.post(`/lecturer/question-versions/${versionId}/rubric/criteria`, payload);
  },

  updateCriterion(versionId, criterionId, payload) {
    return apiClient.put(`/lecturer/question-versions/${versionId}/rubric/criteria/${criterionId}`, payload);
  },

  deleteCriterion(versionId, criterionId, expectedVersion = null) {
    const params = expectedVersion !== null ? { expectedVersion } : {};
    return apiClient.delete(`/lecturer/question-versions/${versionId}/rubric/criteria/${criterionId}`, { params });
  }
};
