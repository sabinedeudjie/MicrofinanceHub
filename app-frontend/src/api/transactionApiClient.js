import axios from 'axios';

const transactionApiClient = axios.create({
  baseURL: 'http://localhost:8088',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

transactionApiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('mfh_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

transactionApiClient.interceptors.response.use(
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

export default transactionApiClient;
