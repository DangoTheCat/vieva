import { apiClient } from './client';

export const adminUserApi = {
  /**
   * @param {Object} params
   * @returns {Promise<PageResponse<UserDto>>}
   */
  async getUsers(params = {}) {
    return apiClient.get('/admin/users', params);
  },

  /**
   * @param {string} id
   * @returns {Promise<UserDto>}
   */
  async getUserById(id) {
    return apiClient.get(`/admin/users/${id}`);
  },

  /**
   * @param {Object} payload
   * @returns {Promise<UserDto>}
   */
  async createUser(payload) {
    return apiClient.post('/admin/users', payload);
  },

  /**
   * @param {string} id
   * @param {Object} payload
   * @returns {Promise<UserDto>}
   */
  async updateUser(id, payload) {
    return apiClient.put(`/admin/users/${id}`, payload);
  },

  /**
   * @param {string} id
   * @returns {Promise<{ message: string }>}
   */
  async deleteUser(id) {
    return apiClient.delete(`/admin/users/${id}`);
  }
};
