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
  },

  /**
   * @param {File} file
   * @returns {Promise<UserDto>}
   */
  async uploadAvatar(file) {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post('/users/me/avatar', formData);
  },

  /**
   * @returns {Promise<UserDto>}
   */
  async deleteAvatar() {
    return apiClient.delete('/users/me/avatar');
  }
};
