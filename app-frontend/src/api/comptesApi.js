import apiClient from './axiosClient';

export const getCompteById = (id) =>
  apiClient.get(`/api/comptes/${id}`);

export const getComptesByClient = (clientId, page = 0, size = 20) =>
  apiClient.get(`/api/comptes/client/${clientId}`, { params: { page, size } });

export const getComptesActifsByClient = (clientId, page = 0, size = 20) =>
  apiClient.get(`/api/comptes/client/${clientId}/actifs`, { params: { page, size } });

export const getSoldeTotalClient = (clientId) =>
  apiClient.get(`/api/comptes/client/${clientId}/solde-total`);

export const consulterSolde = (id) =>
  apiClient.get(`/api/comptes/${id}/solde`);

export const rechercherComptes = (params) =>
  apiClient.get('/api/comptes/recherche', { params });

export const ouvrirCompte = (data) =>
  apiClient.post('/api/comptes', data);

export const modifierCompte = (id, data) =>
  apiClient.put(`/api/comptes/${id}`, data);

export const changerStatut = (id, statut) =>
  apiClient.patch(`/api/comptes/${id}/statut`, null, { params: { statut } });

export const supprimerCompte = (id) =>
  apiClient.delete(`/api/comptes/${id}`);

export const getComptesEnAttenteValidation = (page = 0, size = 50) =>
  apiClient.get('/api/comptes/en-attente-validation', { params: { page, size } });

