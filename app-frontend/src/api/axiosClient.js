import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8082',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// Ajoute automatiquement le token JWT à chaque requête
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('mfh_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Redirige vers /login si le token est expiré ou invalide
apiClient.interceptors.response.use(
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

export default apiClient;