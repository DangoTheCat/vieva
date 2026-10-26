import { apiClient } from './client';

/** Indexed course knowledge (document chunks) used for RAG question generation. */
export const knowledgeApi = {
  /** GET /api/v1/knowledge — semantic search when q is given, otherwise the latest ready chunks */
  search(subjectId, q = '', limit = 10) {
    return apiClient.get('/knowledge', { subjectId, q, limit });
  },

  /** POST /api/v1/knowledge — add a manual knowledge snippet to the subject */
  create({ subjectId, content, topicId }) {
    return apiClient.post('/knowledge', { subjectId, content, topicId: topicId || null });
  }
};
