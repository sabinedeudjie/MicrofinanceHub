import axios from 'axios';

const loansApiClient = axios.create({
  baseURL: 'http://localhost:8083',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

loansApiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('mfh_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const user = (() => {
    try { return JSON.parse(localStorage.getItem('mfh_user') || 'null'); } catch { return null; }
  })();
  if (user?.id) {
    config.headers['X-User-Id'] = String(user.id);
  }
  if (user?.role) {
    config.headers['X-User-Role'] = String(user.role);
  }
  return config;
});

loansApiClient.interceptors.response.use(
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

// ─── Demandes de prêt ─────────────────────────────────────────────────────────

export const applyForLoan = (data) =>
  loansApiClient.post('/api/loans/apply', data);

export const getPendingApplications = (page = 0, size = 10) =>
  loansApiClient.get('/api/loans/applications/pending', { params: { page, size } });

export const getApplication = (applicationId) =>
  loansApiClient.get(`/api/loans/applications/${applicationId}`);

export const getClientApplications = (clientId) =>
  loansApiClient.get(`/api/loans/applications/client/${clientId}`);

export const countPendingForClients = (clientIds) =>
  loansApiClient.post('/api/loans/applications/pending/count', clientIds);

export const getApplicationsByClients = (clientIds) =>
  loansApiClient.post('/api/loans/applications/by-clients', clientIds);

// ─── Prêts ────────────────────────────────────────────────────────────────────

export const getAllLoans = (page = 0, size = 20, status = null) => {
  const params = { page, size };
  if (status) params.status = status;
  return loansApiClient.get('/api/loans/all', { params });
};

export const getLoanById = (loanId) =>
  loansApiClient.get(`/api/loans/${loanId}`);

export const getClientLoans = (clientId) =>
  loansApiClient.get(`/api/loans/client/${clientId}`);

export const getLoanStatus = (loanId) =>
  loansApiClient.get(`/api/loans/${loanId}/status`);

export const getAmortizationSchedule = (loanId) =>
  loansApiClient.get(`/api/loans/${loanId}/amortization`);

export const getLoanSchedules = (loanId) =>
  loansApiClient.get(`/api/loans/${loanId}/schedules`);

// ─── Éligibilité ──────────────────────────────────────────────────────────────

export const checkEligibility = (clientId, amount, termMonths, accountNumber = null) => {
  const params = { amount, termMonths };
  if (accountNumber) params.accountNumber = accountNumber;
  return loansApiClient.get(`/api/loans/eligibility/${clientId}`, { params });
};

// ─── Statistiques ─────────────────────────────────────────────────────────────

export const getLoanStats = (startDate, endDate) =>
  loansApiClient.get('/api/loans/stats', { params: { startDate, endDate } });

export const getPortfolioStats = () =>
  loansApiClient.get('/api/loans/portfolio/stats');

export const getLoanStatsForClients = (clientIds, startDate, endDate) =>
  loansApiClient.post('/api/loans/stats/by-clients', clientIds, { params: { startDate, endDate } });

export const getPortfolioStatsForClients = (clientIds) =>
  loansApiClient.post('/api/loans/portfolio/stats/by-clients', clientIds);

export const getLoanStatsByAgent = (agentId, startDate, endDate) =>
  loansApiClient.post('/api/loans/stats/by-agent', null, { 
    params: { agentId, startDate, endDate },
    headers: { 'X-User-Id': agentId }
  });

export const getPortfolioStatsByAgent = (agentId) =>
  loansApiClient.post('/api/loans/portfolio/stats/by-agent', null, { 
    params: { agentId },
    headers: { 'X-User-Id': agentId }
  });

export const getLoansByClients = (clientIds, page = 0, size = 100, status = null) => {
  const params = { page, size };
  if (status) params.status = status;
  return loansApiClient.post('/api/loans/by-clients', clientIds, { params });
};

export const getPendingApplicationsByClients = (clientIds, page = 0, size = 100) =>
  loansApiClient.post('/api/loans/applications/pending/by-clients', clientIds, { params: { page, size } });

// Helpers dates locales (sans timezone pour Spring LocalDateTime)
const _pad = (n) => String(n).padStart(2, '0');
const _startOfMonth = () => { const d = new Date(); return `${d.getFullYear()}-${_pad(d.getMonth() + 1)}-01T00:00:00`; };
const _now = () => { const d = new Date(); return `${d.getFullYear()}-${_pad(d.getMonth() + 1)}-${_pad(d.getDate())}T${_pad(d.getHours())}:${_pad(d.getMinutes())}:${_pad(d.getSeconds())}`; };

// Stats for agents — params en query string (backend @RequestParam)
export const getLoanStatsForAgent = (agentId, startDate = _startOfMonth(), endDate = _now()) =>
  loansApiClient.post('/api/loans/stats/by-agent', null, { params: { agentId, startDate, endDate } });

export const getLoansByAgent = (agentId) =>
  loansApiClient.post('/api/loans/by-agent', null, { params: { agentId } });

export const getPortfolioStatsForAgent = (agentId) =>
  loansApiClient.post('/api/loans/portfolio/stats/by-agent', null, { params: { agentId } });

// ─── Approbation / Décaissement ───────────────────────────────────────────────

export const approveLoan = (applicationId, data) =>
  loansApiClient.post(`/api/loans/approval/${applicationId}/approve`, data);

export const rejectLoan = (applicationId, data) =>
  loansApiClient.post(`/api/loans/approval/${applicationId}/reject`, data);

export const disburseLoan = (loanId) =>
  loansApiClient.post(`/api/loans/approval/${loanId}/disburse`);

// ─── Produits de prêt (admin) ─────────────────────────────────────────────────

export const getActiveLoanProducts = () =>
  loansApiClient.get('/api/admin/loan-products/active');

export const getAllLoanProducts = (page = 0, size = 20) =>
  loansApiClient.get('/api/admin/loan-products', { params: { page, size } });

export const updateLoanProduct = (id, data) =>
  loansApiClient.put(`/api/admin/loan-products/${id}`, data);

export const createLoanProduct = (data) =>
  loansApiClient.post('/api/admin/loan-products', data);

export const toggleLoanProduct = (id, active) =>
  loansApiClient.patch(`/api/admin/loan-products/${id}/status`, null, { params: { active } });

export default loansApiClient;
