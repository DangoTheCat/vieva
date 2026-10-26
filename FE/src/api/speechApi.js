import { apiClient } from './client';

/** System-wide speech (STT/TTS) settings used by the oral exam room. */
export const speechApi = {
  /** GET /api/v1/admin/speech */
  getAdminSettings() {
    return apiClient.get('/admin/speech');
  },

  /**
   * PATCH /api/v1/admin/speech — omitted fields keep their current value; speed/pitch in 0.25..4.0
   * @param {{ sttLanguage?: string, ttsLanguage?: string, vietnameseVoice?: string, englishVoice?: string, speed?: number, pitch?: number }} payload
   */
  updateSettings(payload) {
    return apiClient.patch('/admin/speech', payload);
  },

  /** GET /api/v1/student/speech — same settings, readable by any signed-in user */
  getSettings() {
    return apiClient.get('/student/speech');
  }
};
