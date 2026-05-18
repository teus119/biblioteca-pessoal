/* ================================================
   auth.js — Gerenciamento de sessão JWT
   ================================================ */

const Auth = {
  save(data) {
    localStorage.setItem('auth_token',    data.token);
    localStorage.setItem('auth_username', data.username);
    localStorage.setItem('auth_email',    data.email);
    localStorage.setItem('auth_userId',   data.userId);
  },

  getToken()    { return localStorage.getItem('auth_token'); },
  getUsername() { return localStorage.getItem('auth_username'); },
  getEmail()    { return localStorage.getItem('auth_email'); },
  getUserId()   { return localStorage.getItem('auth_userId'); },

  isLoggedIn()  { return !!this.getToken(); },

  logout() {
    localStorage.clear();
    window.location.href = 'index.html';
  },

  requireAuth() {
    if (!this.isLoggedIn()) {
      window.location.href = 'index.html';
      return false;
    }
    return true;
  },

  redirectIfLoggedIn() {
    if (this.isLoggedIn()) {
      window.location.href = 'dashboard.html';
    }
  },
};

/* ================================================
   Toast — Sistema de notificações
   ================================================ */

const Toast = {
  _container: null,

  _getContainer() {
    if (!this._container) {
      this._container = document.createElement('div');
      this._container.className = 'toast-container';
      document.body.appendChild(this._container);
    }
    return this._container;
  },

  show(message, type = 'info', duration = 3500) {
    const icons = { success: '✅', error: '❌', info: 'ℹ️', warning: '⚠️' };
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
      <span class="toast-icon">${icons[type] || icons.info}</span>
      <span class="toast-msg">${message}</span>
    `;
    this._getContainer().appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(20px)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, duration);
  },

  success: (msg) => Toast.show(msg, 'success'),
  error:   (msg) => Toast.show(msg, 'error'),
  info:    (msg) => Toast.show(msg, 'info'),
};
