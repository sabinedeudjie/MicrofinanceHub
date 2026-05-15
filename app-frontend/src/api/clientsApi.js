import axios from 'axios';

const clientsApiClient = axios.create({
  baseURL: 'http://localhost:8081',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

clientsApiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('mfh_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

clientsApiClient.interceptors.response.use(
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

// ─── CRUD clients ─────────────────────────────────────────────────────────────

export const getAllClients = () =>
  clientsApiClient.get('/api/clients');

export const getAllClientsPaginated = (page = 0, size = 20) =>
  clientsApiClient.get('/api/clients/paginated', { params: { page, size } });

export const getClientById = (id) =>
  clientsApiClient.get(`/api/clients/${id}`);

export const getMyClientProfile = () =>
  clientsApiClient.get('/api/clients/me');

export const getClientByEmail = (email) =>
  clientsApiClient.get('/api/clients/by-email', { params: { email } });

export const searchClients = (q, page = 0, size = 20) =>
  clientsApiClient.get('/api/clients/search', { params: { q, page, size } });

export const createClient = (data) =>
  clientsApiClient.post('/api/clients', data);

export const updateClient = (id, data) =>
  clientsApiClient.put(`/api/clients/${id}`, data);

export const updateClientStatus = (id, status) =>
  clientsApiClient.patch(`/api/clients/${id}/status`, null, { params: { status } });

export const deleteClient = (id) =>
  clientsApiClient.delete(`/api/clients/${id}`);

export const getClientStats = () =>
  clientsApiClient.get('/api/clients/stats');

export const getMyClients = () =>
  clientsApiClient.get('/api/clients/my-clients');

export const getMyClientsStats = () =>
  clientsApiClient.get('/api/clients/my-clients/stats');

export const getClientsByAgent = (agentIdentifier) =>
  clientsApiClient.get(`/api/clients/by-agent/${agentIdentifier}`);

export const getCreditScore = (clientId) =>
  clientsApiClient.get(`/api/clients/${clientId}/credit-score`);

export const checkClientExistsByEmail = (email) =>
  clientsApiClient.get('/api/clients/exists/by-email', { params: { email } });

export const getClientsByAgency = (agencyId) =>
  clientsApiClient.get(`/api/clients/by-agency/${agencyId}`);

export const assignClientToAgent = (clientId, agentEmail, agencyId) =>
  clientsApiClient.patch(`/api/clients/${clientId}/assign-agent`, { agentEmail, agencyId });

export default clientsApiClient;
