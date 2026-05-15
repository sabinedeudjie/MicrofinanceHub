import axios from 'axios';

const notificationsApiClient = axios.create({
  baseURL: 'http://localhost:8089',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

notificationsApiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('mfh_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

notificationsApiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('mfh_token');
      localStorage.removeItem('mfh_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// ─── Admin ────────────────────────────────────────────────────────────────────

export const getAllNotifications = (page = 0, size = 20) =>
  notificationsApiClient.get('/api/v1/notifications/admin/all', { params: { page, size } });

export const getNotificationStats = () =>
  notificationsApiClient.get('/api/v1/notifications/admin/stats');

export const sendNotification = (data) =>
  notificationsApiClient.post('/api/v1/notifications', data);

// ─── Client ───────────────────────────────────────────────────────────────────

export const getNotificationsByClient = (clientId, page = 0, size = 20) =>
  notificationsApiClient.get(`/api/v1/notifications/client/${clientId}`, { params: { page, size } });

export const getUnreadCount = (clientId) =>
  notificationsApiClient.get(`/api/v1/notifications/client/${clientId}/non-lues`);

export const markAsRead = (id) =>
  notificationsApiClient.patch(`/api/v1/notifications/${id}/lue`);

export const retryEchecs = () =>
  notificationsApiClient.post('/api/v1/notifications/retry');

export default notificationsApiClient;
