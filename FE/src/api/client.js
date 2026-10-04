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

    const config = {
      method: options.method || 'GET',
      headers,
      ...options,
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

        throw new ApiError(errorMessage, errorCode, response.status, data);
      }

      return data;
    } catch (err) {
      if (err instanceof ApiError) {
        throw err;
      }
      throw new ApiError(
        'Không thể kết nối đến máy chủ Backend. Vui lòng kiểm tra dịch vụ Spring Boot (port 8080).',
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
