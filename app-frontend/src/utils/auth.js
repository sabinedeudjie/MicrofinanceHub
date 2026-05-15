/**
 * auth.js — Gestion de la session locale (localStorage)
 *
 * Clés utilisées :
 *   mfh_token       : token JWT retourné par le backend
 *   mfh_user        : objet { id, email, role, firstName, lastName, prenom, nom, clientId? }
 *   mfh_login_count : compteur de connexions (pour la salutation dynamique)
 */

/** Sauvegarde le token et les infos utilisateur après un login réussi */
export const saveSession = (token, user) => {
  const previousCount = parseInt(localStorage.getItem('mfh_login_count') || '0');
  localStorage.setItem('mfh_login_count', String(previousCount + 1));
  localStorage.setItem('mfh_token', token);
  localStorage.setItem('mfh_user', JSON.stringify(user));
};
/** Retourne le token JWT stocké, ou null */
export const getToken = () => localStorage.getItem('mfh_token');

/** Retourne l'utilisateur connecté, ou null */
export const getCurrentUser = () => {
  try {
    const raw = localStorage.getItem('mfh_user');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

/** Déconnecte l'utilisateur (supprime token + infos mais garde le compteur) */
export const logout = () => {
  localStorage.removeItem('mfh_token');
  localStorage.removeItem('mfh_user');
};

/**
 * Salutation personnalisée selon le nombre de connexions.
 * Première visite → "Bienvenue, [Prénom] !"
 * Retours suivants → salutation familière
 */
export const getGreeting = (prenom) => {
  const count = parseInt(localStorage.getItem('mfh_login_count') || '1');

  if (count <= 1) return `Bienvenue, ${prenom} !`;

  const salutations = [
    `Bon retour, ${prenom} !`,
    `Content de vous revoir, ${prenom} !`,
    `Ravi de vous retrouver, ${prenom} !`,
    `De retour parmi nous, ${prenom} !`,
    `Heureux de vous revoir, ${prenom} !`,
  ];

  return salutations[(count - 2) % salutations.length];
};
