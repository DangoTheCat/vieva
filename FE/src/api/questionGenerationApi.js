import { apiClient } from './client';

export const questionGenerationApi = {
  generate(payload) {
    return apiClient.post('/lecturer/question-generation-requests', payload);
  },

  getRequest(requestId) {
    return apiClient.get(`/lecturer/question-generation-requests/${requestId}`);
  },

  retry(requestId) {
    return apiClient.post(`/lecturer/question-generation-requests/${requestId}/retry`);
  }
};
