import { apiClient } from './client';

export const authApi = {
  /**
   * @param {Object} credentials
   * @returns {Promise<{ accessToken, tokenType, expiresIn, user }>}
   */
  async login(credentials) {
    const data = await apiClient.post('/auth/login', credentials);
    if (data?.accessToken) {
      apiClient.setToken(data.accessToken);
    }
    return data;
  },

  /**
   * @param {Object} payload
   * @returns {Promise<{ accessToken, tokenType, expiresIn, user }>}
   */
  async register(payload) {
    const data = await apiClient.post('/auth/register', payload);
    if (data?.accessToken) {
      apiClient.setToken(data.accessToken);
    }
    return data;
  },

  logout() {
    apiClient.setToken(null);
    localStorage.removeItem('aives_user');
  }
};
