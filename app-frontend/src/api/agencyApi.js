import axios from 'axios';

const agencyApiClient = axios.create({
  baseURL: 'http://localhost:8086',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

agencyApiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('mfh_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

agencyApiClient.interceptors.response.use(
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

export const getAllAgencies = () =>
  agencyApiClient.get('/api/admin/agencies');

export const getAgencyById = (id) =>
  agencyApiClient.get(`/api/admin/agencies/${id}`);

export const createAgency = (data) =>
  agencyApiClient.post('/api/admin/agencies', data);

export const updateAgency = (id, data) =>
  agencyApiClient.put(`/api/admin/agencies/${id}`, data);

export const deleteAgency = (id) =>
  agencyApiClient.delete(`/api/admin/agencies/${id}`);

export const assignDirecteur = (agencyId, directorId) =>
  agencyApiClient.patch(`/api/admin/agencies/${agencyId}/director/${directorId}`);

export const toggleAgencyStatus = (agencyId) =>
  agencyApiClient.patch(`/api/admin/agencies/${agencyId}/toggle`);

export const assignAgent = (agencyId, agentId) =>
  agencyApiClient.post('/api/admin/agencies/assign-agent', { agencyId, agentId });

export const getAllActiveAssignments = () =>
  agencyApiClient.get('/api/admin/agencies/agents/assignments');

export const getMyAgency = () =>
  agencyApiClient.get('/api/agency/my-agency');

export const getMyAgencyAgents = () =>
  agencyApiClient.get('/api/agency/my-agents');

export const getMyAgencyClients = () =>
  agencyApiClient.get('/api/agency/my-clients');

export const getMyAgencyClientsStats = () =>
  agencyApiClient.get('/api/agency/my-clients/stats');

export const unassignMyAgent = (agentId) =>
  agencyApiClient.delete(`/api/agency/agents/${agentId}/unassign`);

export const unassignAgentAdmin = (agentId) =>
  agencyApiClient.delete(`/api/admin/agencies/agents/${agentId}/unassign`);

export const unassignDirectorAdmin = (agencyId) =>
  agencyApiClient.delete(`/api/admin/agencies/${agencyId}/director`);

export const getAgencyAgents = (agencyId) =>
  agencyApiClient.get(`/api/admin/agencies/${agencyId}/agents`);

export const getAgencyStats = (agencyId) =>
  agencyApiClient.get(`/api/admin/agencies/${agencyId}/stats`);

export const getAgencyByIdInternal = (agencyId) =>
  agencyApiClient.get(`/api/internal/agencies/${agencyId}`);

export const getAgentAgencyInternal = (agentId) =>
  agencyApiClient.get(`/api/internal/agencies/agent/${agentId}`);

export const toggleMyAgentStatus = (agentId) =>
  agencyApiClient.patch(`/api/agency/agents/${agentId}/toggle-status`);

export default agencyApiClient;
