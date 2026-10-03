import { apiClient } from './client';

export const lecturerCatalogApi = {
  getAssignedSubjects() {
    return apiClient.get('/lecturer/subjects');
  },

  getTopics(subjectId) {
    return apiClient.get(`/lecturer/subjects/${subjectId}/topics`);
  },

  createTopic(subjectId, { name, description }) {
    return apiClient.post(`/lecturer/subjects/${subjectId}/topics`, { name, description });
  }
};
