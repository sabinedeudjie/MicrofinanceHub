import axios from 'axios';

const repaymentApiClient = axios.create({
  baseURL: 'http://localhost:8084',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

repaymentApiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('mfh_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;

  const user = (() => {
    try { return JSON.parse(localStorage.getItem('mfh_user') || 'null'); } catch { return null; }
  })();
  if (user) {
    // Pour les clients : le backend compare X-User-Id au clientId du prêt
    config.headers['X-User-Id']   = String(user.clientId ?? user.id ?? '');
    config.headers['X-User-Role'] = String(user.role ?? '');
  }
  return config;
});

repaymentApiClient.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('mfh_token');
      localStorage.removeItem('mfh_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Helpers dates — sans timezone (LocalDateTime côté Spring)
const pad = (n) => String(n).padStart(2, '0');
const startOfMonth = () => {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-01T00:00:00`;
};
const now = () => {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

// ─── Paiement client ──────────────────────────────────────────────────────────
export const clientMakePayment = (data) =>
  repaymentApiClient.post('/api/repayments/pay/client', data);

// ─── Paiement enregistré par agent/directeur/admin ───────────────────────────
export const agentRecordPayment = (data) =>
  repaymentApiClient.post('/api/repayments/pay/record', data);

// ─── Statut & historique de remboursement d'un prêt ─────────────────────────
export const getRepaymentStatus = (loanId) =>
  repaymentApiClient.get(`/api/repayments/loan/${loanId}`);

// ─── Stats globales (admin) ───────────────────────────────────────────────────
export const getRepaymentStats = (startDate = startOfMonth(), endDate = now()) =>
  repaymentApiClient.get('/api/repayments/stats', { params: { startDate, endDate } });

// ─── Stats par agent ─────────────────────────────────────────────────────────
export const getRepaymentStatsForAgent = (agentId, startDate = startOfMonth(), endDate = now()) =>
  repaymentApiClient.post('/api/repayments/stats/by-agent', null, {
    params: { agentId, startDate, endDate },
  });

// ─── Total collecté par agent ─────────────────────────────────────────────────
export const getTotalRepaymentsForAgent = (agentId, startDate = startOfMonth(), endDate = now()) =>
  repaymentApiClient.get(`/api/repayments/by-agent/${agentId}/total`, {
    params: { startDate, endDate },
  });

// ─── Stats pour liste de clients ─────────────────────────────────────────────
export const getRepaymentStatsForClients = (clientIds, startDate = startOfMonth(), endDate = now()) =>
  repaymentApiClient.post('/api/repayments/stats/by-clients', clientIds, {
    params: { startDate, endDate },
  });

export const getTotalRepaymentsForClients = (clientIds, startDate = startOfMonth(), endDate = now()) =>
  repaymentApiClient.post('/api/repayments/total/by-clients', clientIds, {
    params: { startDate, endDate },
  });

export const validatePayment = (paymentId) =>
  repaymentApiClient.post(`/api/repayments/${paymentId}/validate`);

export const getPendingPayments = () =>
  repaymentApiClient.get('/api/repayments/pending');

export default repaymentApiClient;
