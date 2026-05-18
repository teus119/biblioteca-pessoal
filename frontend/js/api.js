/* ================================================
   api.js — Wrapper para chamadas REST ao backend
   ================================================ */

const API_BASE = 'http://localhost:8080/api';

const api = {
  _getHeaders() {
    const token = localStorage.getItem('auth_token');
    return {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    };
  },

  async _request(method, path, body = null) {
    const options = {
      method,
      headers: this._getHeaders(),
    };
    if (body) options.body = JSON.stringify(body);

    const response = await fetch(`${API_BASE}${path}`, options);

    if ((response.status === 403 || response.status === 401) && !path.includes('/auth/login') && !path.includes('/auth/register')) {
      // Token expirado ou inválido — redirecionar para login
      localStorage.clear();
      window.location.href = 'index.html';
      return;
    }

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

    if (!response.ok) {
      const error = new Error(data?.message || data?.error || 'Erro desconhecido');
      error.status = response.status;
      error.fields = data?.fields || null;
      throw error;
    }

    return data;
  },

  // ===== AUTH =====
  auth: {
    register: (body) => api._request('POST', '/auth/register', body),
    login:    (body) => api._request('POST', '/auth/login', body),
    me:       ()     => api._request('GET',  '/auth/me'),
  },

  // ===== BOOKS =====
  books: {
    list:   ()         => api._request('GET',    '/books'),
    get:    (id)       => api._request('GET',    `/books/${id}`),
    create: (body)     => api._request('POST',   '/books', body),
    update: (id, body) => api._request('PUT',    `/books/${id}`, body),
    delete: (id)       => api._request('DELETE', `/books/${id}`),
    search: (q)        => api._request('GET',    `/books/search?q=${encodeURIComponent(q)}`),
    stats:  ()         => api._request('GET',    '/books/stats'),
  },
};
