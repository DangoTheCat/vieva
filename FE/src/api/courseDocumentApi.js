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
  },

  getDocumentPreview(documentId) {
    return apiClient.get(`/lecturer/documents/${documentId}/preview`);
  },

  /** PATCH /lecturer/documents/{id} — assign the document to a syllabus topic (null clears it) */
  updateMetadata(documentId, { topicId, metadataJson } = {}) {
    return apiClient.patch(`/lecturer/documents/${documentId}`, { topicId: topicId || null, metadataJson: metadataJson ?? null });
  }
};
