import authApiClient from './authApiClient';
import clientsApiClient from './clientsApi';
import { saveSession } from '../utils/auth';

// ─── Login ─────────────────────────────────────────────────────────────────────

export const loginWithCredentials = async (email, password) => {
  const normalizedEmail = email?.trim().toLowerCase();

  try {
    // 1. Authentification via auth-service
    const loginRes = await authApiClient.post('/auth/login', {
      email: normalizedEmail,
      password,
    });

    const { access_token, role, firstName, lastName, agency_id, status } = loginRes.data;

    // 2. Récupérer l'ID de l'utilisateur via /auth/me
    const meRes = await authApiClient.get('/auth/me', {
      headers: { Authorization: `Bearer ${access_token}` },
    });
    const authUserId = meRes.data.id;

    // 3. Pour un CLIENT : récupérer aussi l'ID client-service
    let clientId = null;
    if (role === 'CLIENT') {
      try {
        const clientRes = await clientsApiClient.get('/api/clients/me', {
          headers: { Authorization: `Bearer ${access_token}` },
        });
        clientId = clientRes.data.id;
      } catch {
        // Le profil client peut ne pas encore exister, ce n'est pas bloquant
      }
    }

    const user = {
      id: authUserId,
      email: normalizedEmail,
      role: role.toLowerCase(),
      firstName,
      lastName,
      prenom: firstName,
      nom: lastName,
      status,
      ...(clientId && { clientId }),
      ...(agency_id && { agencyId: agency_id }),
    };

    saveSession(access_token, user);

    const redirectMap = {
      admin: '/admin/dashboard',
      agent: '/agent/dashboard',
      client: '/client/espace',
      directeur_agence: '/directeur/dashboard',
    };
    return { success: true, redirectTo: redirectMap[role.toLowerCase()] ?? '/login' };

  } catch (error) {
    const status = error.response?.status;
    const serverMsg = error.response?.data?.message;
    if (status === 401 || status === 403) {
      return { success: false, error: serverMsg || 'Email ou mot de passe incorrect. Veuillez réessayer.' };
    }
    return { success: false, error: serverMsg || 'Impossible de joindre le serveur. Vérifiez que les services sont démarrés.' };
  }
};

// ─── Inscription client (auto-service) ────────────────────────────────────────
// Le client doit déjà exister dans client-service (créé par un agent).
// Cette fonction crée uniquement le compte auth.

export const registerClient = async (form) => {
  try {
    await authApiClient.post('/auth/register', {
      email: form.email?.trim().toLowerCase(),
      password: form.motDePasse,
    });
    return { success: true };
  } catch (error) {
    const msg = error.response?.data?.message
      || error.response?.data
      || "Inscription impossible. Vérifiez que votre email a bien été enregistré par votre agence.";
    return { success: false, error: String(msg) };
  }
};


// ─── Création d'un agent par l'admin ──────────────────────────────────────────

export const registerAgent = async (form) => {
  try {
    const res = await authApiClient.post('/auth/agent/create', {
      email: form.email?.trim().toLowerCase(),
      password: form.motDePasse,
      firstName: form.prenom,
      lastName: form.nom,
      phoneNumber: form.telephone?.trim() || null,
    });
    return { success: true, agent: res.data };
  } catch (error) {
    const msg = error.response?.data?.message
      || error.response?.data
      || "Erreur lors de la création de l'agent.";
    return { success: false, error: String(msg) };
  }
};

// ─── Création d'un directeur par l'admin ──────────────────────────────────────

export const registerDirecteur = async (form) => {
  try {
    const res = await authApiClient.post('/auth/admin/create-directeur', {
      email: form.email?.trim().toLowerCase(),
      password: form.motDePasse,
      firstName: form.prenom,
      lastName: form.nom,
      phoneNumber: form.telephone?.trim() || null,
    });
    return { success: true, directeur: res.data };
  } catch (error) {
    const msg = error.response?.data?.message
      || error.response?.data
      || "Erreur lors de la création du directeur.";
    return { success: false, error: String(msg) };
  }
};

export const registerAdmin = async (form) => {
  try {
    const res = await authApiClient.post('/auth/admin/create', {
      email: form.email?.trim().toLowerCase(),
      password: form.motDePasse,
      firstName: form.prenom,
      lastName: form.nom,
      phoneNumber: form.telephone?.trim() || null,
    });
    return { success: true, admin: res.data };
  } catch (error) {
    const msg = error.response?.data?.message
      || error.response?.data
      || "Erreur lors de la création de l'administrateur.";
    return { success: false, error: String(msg) };
  }
};

// ─── Lecture des utilisateurs (pour pages admin) ──────────────────────────────

export const getAgentById = (id) =>
  authApiClient.get(`/auth/users/${id}`);

export const getUserByEmail = (email) =>
  authApiClient.get('/auth/users/by-email', { params: { email } });

export const getPublicUserById = (id) =>
  authApiClient.get(`/api/public/users/${id}`);

export const getPublicUserByEmail = (email) =>
  authApiClient.get('/api/public/users/by-email', { params: { email } });

// ─── CRUD utilisateurs (admin) ────────────────────────────────────────────────

export const updateUser = (id, data) =>
  authApiClient.put(`/auth/users/${id}`, data);

export const deleteUser = (id) =>
  authApiClient.delete(`/auth/users/${id}`);

export const toggleUser = (id) =>
  authApiClient.patch(`/auth/users/${id}/toggle`);

// ─── Déconnexion ──────────────────────────────────────────────────────────────

export const logoutFromServer = async () => {
  try {
    await authApiClient.post('/auth/logout');
  } catch {
    // Erreur silencieuse — on nettoie localStorage quand même
  }
  localStorage.removeItem('mfh_token');
  localStorage.removeItem('mfh_user');
};

// ─── Mot de passe oublié ──────────────────────────────────────────────────────

export const requestPasswordReset = async (email) => {
  try {
    const res = await authApiClient.post('/auth/forgot-password', { email });
    return { success: true, message: res.data.message };
  } catch (error) {
    const msg = error.response?.data?.error
      || error.response?.data?.message
      || "Une erreur est survenue. Veuillez réessayer.";
    return { success: false, error: String(msg) };
  }
};

export const resetPassword = async (token, newPassword) => {
  try {
    const res = await authApiClient.post('/auth/reset-password', { token, newPassword });
    return { success: true, message: res.data.message };
  } catch (error) {
    const msg = error.response?.data?.error
      || error.response?.data?.message
      || "Une erreur est survenue. Veuillez réessayer.";
    return { success: false, error: String(msg) };
  }
};
