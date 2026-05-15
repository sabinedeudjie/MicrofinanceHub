import transactionApiClient from './transactionApiClient';

// ─── Opérations financières ─────────────────────────────────────────────────

export const effectuerDepot = (compteId, data) =>
  transactionApiClient.post(`/api/transactions/depot/${compteId}`, data);

export const effectuerRetrait = (compteId, data) =>
  transactionApiClient.post(`/api/transactions/retrait/${compteId}`, data);

export const effectuerVirement = (compteId, data) =>
  transactionApiClient.post(`/api/transactions/virement/${compteId}`, data);

// ─── Historique par compte ───────────────────────────────────────────────────

export const getTransactions = (compteId, page = 0, size = 20) =>
  transactionApiClient.get(`/api/transactions/compte/${compteId}`, { params: { page, size } });

export const getTransactionsByPeriode = (compteId, debut, fin, page = 0, size = 20) =>
  transactionApiClient.get(`/api/transactions/compte/${compteId}/periode`, { params: { debut, fin, page, size } });

export const getTransactionsByType = (compteId, type, page = 0, size = 20) =>
  transactionApiClient.get(`/api/transactions/compte/${compteId}/type/${type}`, { params: { page, size } });

export const compterTransactions = (compteId) =>
  transactionApiClient.get(`/api/transactions/compte/${compteId}/count`);

// ─── Supervision admin ───────────────────────────────────────────────────────

export const rechercherTransactions = (params) =>
  transactionApiClient.get('/api/transactions/recherche', { params });

export const getTransactionsByStatut = (statut, page = 0, size = 20) =>
  transactionApiClient.get(`/api/transactions/statut/${statut}`, { params: { page, size } });

export const relancerTransactions = () =>
  transactionApiClient.post('/api/transactions/relance');
