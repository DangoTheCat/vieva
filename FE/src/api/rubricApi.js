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
  },

  // Reusable rubric templates per subject; criteriaJson is a JSON array of
  // { name, description, maxScore, levels: [{ label, description, score }], orderIndex }

  listTemplates(subjectId) {
    return apiClient.get(`/lecturer/subjects/${subjectId}/rubric-templates`);
  },

  createTemplate(subjectId, { name, description, criteriaJson }) {
    return apiClient.post(`/lecturer/subjects/${subjectId}/rubric-templates`, { name, description, criteriaJson });
  },

  updateTemplate(subjectId, templateId, { name, description, criteriaJson }) {
    return apiClient.put(`/lecturer/subjects/${subjectId}/rubric-templates/${templateId}`, { name, description, criteriaJson });
  },

  deleteTemplate(subjectId, templateId) {
    return apiClient.delete(`/lecturer/subjects/${subjectId}/rubric-templates/${templateId}`);
  },

  /** Replaces the DRAFT version's rubric with the template's criteria */
  applyTemplate(subjectId, templateId, versionId) {
    return apiClient.post(`/lecturer/subjects/${subjectId}/rubric-templates/${templateId}/copy-to-version/${versionId}`);
  }
};
