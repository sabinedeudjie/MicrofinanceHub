import axios from 'axios';

const authApiClient = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

authApiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('mfh_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

authApiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginAttempt = error.config?.url?.includes('/auth/login');
    if (error.response?.status === 401 && !isLoginAttempt) {
      localStorage.removeItem('mfh_token');
      localStorage.removeItem('mfh_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default authApiClient;
