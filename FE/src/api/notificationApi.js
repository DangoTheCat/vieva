import { apiClient } from './client';

export const notificationApi = {
  /** GET /api/v1/notifications — newest first, paged (size <= 100) */
  list(page = 0, size = 20) {
    return apiClient.get('/notifications', { page, size });
  },

  /** GET /api/v1/notifications/unread-count -> { count } */
  unreadCount() {
    return apiClient.get('/notifications/unread-count');
  },

  /** PATCH /api/v1/notifications/{id}/read */
  markRead(notificationId) {
    return apiClient.patch(`/notifications/${notificationId}/read`);
  }
};
