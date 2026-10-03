import { apiClient } from './client';

export const courseDocumentApi = {
  uploadDocument(subjectId, file) {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post(`/lecturer/subjects/${subjectId}/documents`, formData);
  },

  getDocuments(subjectId) {
    return apiClient.get(`/lecturer/subjects/${subjectId}/documents`);
  },

  getDocumentById(documentId) {
    return apiClient.get(`/lecturer/documents/${documentId}`);
  },

  retryIndexing(documentId) {
    return apiClient.post(`/lecturer/documents/${documentId}/retry-index`);
  },

  deleteDocument(documentId) {
    return apiClient.delete(`/lecturer/documents/${documentId}`);
  }
};
