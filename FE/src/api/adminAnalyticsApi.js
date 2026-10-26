import { apiClient } from './client';

export const adminAnalyticsApi = {
  /** GET /api/v1/admin/stats — user, exam and attempt counters */
  getStats() {
    return apiClient.get('/admin/stats');
  },

  /**
   * GET /api/v1/admin/performance — paged completed-attempt results
   * @param {{ studentId?: string, subjectId?: string, examId?: string, status?: string, page?: number, size?: number }} params
   */
  getPerformance(params = {}) {
    return apiClient.get('/admin/performance', params);
  }
};
