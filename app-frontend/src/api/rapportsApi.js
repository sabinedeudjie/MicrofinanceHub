import axios from 'axios';

const rapportsApiClient = axios.create({
  baseURL: 'http://localhost:8085',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

rapportsApiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('mfh_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

rapportsApiClient.interceptors.response.use(
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

export const getDashboardStats = () =>
  rapportsApiClient.get('/api/reporting/dashboard');

export const getGlobalStats = (startDate, endDate) =>
  rapportsApiClient.get('/api/reporting/stats', { params: { startDate, endDate } });

export const getPortfolioReport = () =>
  rapportsApiClient.get('/api/reporting/portfolio');

export const getAgentReport = (agentId, startDate, endDate) =>
  rapportsApiClient.get(`/api/reporting/agent/${agentId}`, { params: { startDate, endDate } });

export const exportReport = (type, startDate, endDate) =>
  rapportsApiClient.get('/api/reporting/export', {
    params: { type, startDate, endDate },
    responseType: 'blob',
  });

export default rapportsApiClient;
