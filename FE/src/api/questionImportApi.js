import { apiClient } from './client';

export const questionImportApi = {
  /**
   * POST /lecturer/subjects/{subjectId}/questions/import-text — one question per non-blank line (max 2000).
   * Creates drafts in the review queue; returns { totalImported, questionIds }.
   */
  importText(subjectId, { text, topicId, bloomLevel, rubricTemplateId }) {
    return apiClient.post(`/lecturer/subjects/${subjectId}/questions/import-text`, {
      text,
      topicId: topicId || null,
      bloomLevel: bloomLevel || null,
      rubricTemplateId: rubricTemplateId || null
    });
  },

  downloadTemplate(format = 'xlsx') {
    return apiClient.getBlob('/lecturer/import-templates/questions', { format });
  },

  importQuestions(subjectId, file, dryRun = true) {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post(`/lecturer/subjects/${subjectId}/questions/import?dryRun=${dryRun}`, formData);
  }
};
