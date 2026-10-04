import { apiClient } from './client';

export const aiAssistantApi = {
  /**
   * @param {string} message
   * @returns {Promise<{ reply: string, snapshotGeneratedAt: string }>}
   */
  chat(message) {
    return apiClient.post('/ai/assistant/chat', { message });
  }
};
