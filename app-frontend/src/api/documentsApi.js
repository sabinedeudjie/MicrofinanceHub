import axios from 'axios';

const documentsApiClient = axios.create({
  baseURL: 'http://localhost:8081',
  timeout: 30000,
});

documentsApiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('mfh_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

documentsApiClient.interceptors.response.use(
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

export const getClientDocuments = (clientId) =>
  documentsApiClient.get(`/api/clients/${clientId}/documents`);

export const uploadDocument = (clientId, file, type) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('type', type);
  return documentsApiClient.post(`/api/clients/${clientId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const verifyDocument = (docId, status) =>
  documentsApiClient.patch(`/api/documents/${docId}/verify`, null, { params: { status } });

export const getDocumentFileUrl = (fileUrl) => fileUrl;

export default documentsApiClient;
