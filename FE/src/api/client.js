const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export class ApiError extends Error {
  constructor(message, code, status, data) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
    this.data = data;
  }
}

class ApiClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
  }

  /** Role flag published by AuthContext after login; only used to explain a forbidden action early. */
  isAdminSession() {
    try {
      return window.sessionStorage.getItem('aives_role') === 'ADMIN';
    } catch (e) {
      return false;
    }
  }

  getToken() {
    return localStorage.getItem('aives_token');
  }

  setToken(token) {
    if (token) {
      localStorage.setItem('aives_token', token);
    } else {
      localStorage.removeItem('aives_token');
    }
  }

  async request(endpoint, options = {}) {
    const url = new URL(this.baseUrl + endpoint, window.location.origin);

    if (options.params) {
      Object.entries(options.params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          url.searchParams.append(key, value);
        }
      });
    }

    const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;

    const headers = {
      ...(!isFormData && { 'Content-Type': 'application/json' }),
      ...options.headers,
    };

    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    // Admins have read-only access to lecturer data; the BE rejects writes with 403 (plan item 3.3)
    const method = (options.method || 'GET').toUpperCase();
    if (method !== 'GET' && endpoint.startsWith('/lecturer/') && this.isAdminSession()) {
      throw new ApiError(
        'Quản trị viên chỉ được xem dữ liệu của giảng viên. Thao tác này do giảng viên phụ trách môn thực hiện.',
        'ADMIN_READ_ONLY',
        403,
        null
      );
    }

    const config = {
      ...options,
      method: options.method || 'GET',
      headers,
    };

    if (options.body && typeof options.body === 'object' && !isFormData) {
      config.body = JSON.stringify(options.body);
    } else if (isFormData) {
      config.body = options.body;
    }

    try {
      const response = await fetch(url.toString(), config);

      if (response.status === 204) {
        return null;
      }

      if (options.responseType === 'blob') {
        if (!response.ok) {
          throw new ApiError('Tải tệp tin thất bại', `${response.status}`, response.status, null);
        }
        return await response.blob();
      }

      const contentType = response.headers.get('content-type');
      let data = null;
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
      } else {
        const text = await response.text();
        data = text ? { message: text } : {};
      }

      if (!response.ok) {
        const errorCode = data?.code || `${response.status}`;
        const errorMessage = data?.message || response.statusText || 'Yêu cầu thất bại';
        
        // A 401 from /auth/* means bad credentials, not an expired session
        if (response.status === 401 && !endpoint.startsWith('/auth/')) {
          window.dispatchEvent(new CustomEvent('aives:unauthorized'));
        }
        // Admin-issued password not changed yet: BE blocks everything but the password change
        if (response.status === 403 && errorCode === '1062') {
          window.dispatchEvent(new CustomEvent('aives:password-change-required'));
        }

        throw new ApiError(errorMessage, errorCode, response.status, data);
      }

      return data;
    } catch (err) {
      if (err instanceof ApiError) {
        throw err;
      }
      throw new ApiError(
        'Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại.',
        'NETWORK_ERROR',
        0,
        null
      );
    }
  }

  get(endpoint, params = {}) {
    return this.request(endpoint, { method: 'GET', params });
  }

  getBlob(endpoint, params = {}) {
    return this.request(endpoint, { method: 'GET', params, responseType: 'blob' });
  }

  post(endpoint, body = {}, options = {}) {
    return this.request(endpoint, { method: 'POST', body, ...options });
  }

  put(endpoint, body = {}, options = {}) {
    return this.request(endpoint, { method: 'PUT', body, ...options });
  }

  patch(endpoint, body = {}, options = {}) {
    return this.request(endpoint, { method: 'PATCH', body, ...options });
  }

  delete(endpoint, options = {}) {
    return this.request(endpoint, { method: 'DELETE', ...options });
  }
}

export const apiClient = new ApiClient(BASE_URL);
