import { apiClient } from './client';

export const questionImportApi = {
  downloadTemplate(format = 'xlsx') {
    return apiClient.getBlob('/lecturer/import-templates/questions', { format });
  },

  importQuestions(subjectId, file, dryRun = true) {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post(`/lecturer/subjects/${subjectId}/questions/import?dryRun=${dryRun}`, formData);
  }
};
