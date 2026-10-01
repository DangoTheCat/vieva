import { apiClient } from './client';

export const userApi = {
  /**
   * @returns {Promise<UserDto>}
   */
  async getCurrentUser() {
    return apiClient.get('/users/me');
  },

  /**
   * @param {Object} payload 
   * @returns {Promise<{ message: string }>}
   */
  async updateProfile(payload) {
    return apiClient.patch('/users/me', payload);
  },

  /**
   * @param {Object} payload 
   * @returns {Promise<{ message: string }>}
   */
  async changePassword(payload) {
    return apiClient.put('/users/me/password', payload);
  },

  /**
   * @param {string} id
   * @returns {Promise<UserDto>}
   */
  async getUserById(id) {
    return apiClient.get(`/users/${id}`);
  }
};
