import React, { useState } from 'react';
import { User, Lock, Save } from 'lucide-react';
import { getCurrentUser } from '../../utils/auth';
import authApiClient from '../../api/authApiClient';

const AgentProfilPage = () => {
  const user = getCurrentUser();

  const [mdpForm, setMdpForm] = useState({ ancien: '', nouveau: '', confirmer: '' });
  const [success, setSuccess] = useState('');
  const [error,   setError]   = useState('');
  const [saving,  setSaving]  = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (mdpForm.nouveau !== mdpForm.confirmer) {
      setError('Les mots de passe ne correspondent pas');
      return;
    }
    setSaving(true);
    try {
      await authApiClient.put('/auth/me/password', {
        currentPassword: mdpForm.ancien,
        newPassword:     mdpForm.nouveau,
      });
      setSuccess('Mot de passe modifié avec succès');
      setMdpForm({ ancien: '', nouveau: '', confirmer: '' });
    } catch (err) {
      setError(err.response?.data?.message ?? 'Erreur lors du changement de mot de passe');
    } finally {
      setSaving(false);
    }
  };

  const v = (k) => user?.[k] || '—';

  return (
    <div className="p-6 space-y-5">
      <h1 className="text-2xl font-bold text-gray-800">Mon Profil</h1>

      <div className="grid grid-cols-2 gap-5">

        <div className="card">
          <div className="flex items-center gap-4 mb-5">
            <div className="w-14 h-14 bg-blue-100 rounded-full flex items-center justify-center">
              <User size={24} className="text-blue-600" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-gray-800">
                {v('firstName') !== '—' ? v('firstName') : v('prenom')} {v('lastName') !== '—' ? v('lastName') : v('nom')}
              </h2>
              <p className="text-sm text-gray-400">Agent Terrain</p>
              <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full">Actif</span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 text-sm">
            {[
              ['Prénom',   user?.firstName ?? user?.prenom],
              ['Nom',      user?.lastName  ?? user?.nom],
              ['Email',    user?.email],
              ['Rôle',     'Agent Terrain'],
              ['ID',       user?.id?.slice(0, 12) + '…'],
            ].map(([label, value]) => (
              <div key={label}>
                <p className="text-xs text-gray-400">{label}</p>
                <p className="font-medium text-gray-700">{value || '—'}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="card">
          <h3 className="font-semibold text-gray-700 flex items-center gap-2 mb-5">
            <Lock size={16} className="text-blue-500" /> Changer mon Mot de Passe
          </h3>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="form-label">Mot de Passe Actuel *</label>
              <input type="password" value={mdpForm.ancien}
                onChange={e => setMdpForm(p => ({ ...p, ancien: e.target.value }))}
                placeholder="••••••••" required className="form-input" />
            </div>
            <div>
              <label className="form-label">Nouveau Mot de Passe *</label>
              <input type="password" value={mdpForm.nouveau}
                onChange={e => setMdpForm(p => ({ ...p, nouveau: e.target.value }))}
                placeholder="Minimum 6 caractères" required minLength={6} className="form-input" />
            </div>
            <div>
              <label className="form-label">Confirmer *</label>
              <input type="password" value={mdpForm.confirmer}
                onChange={e => setMdpForm(p => ({ ...p, confirmer: e.target.value }))}
                placeholder="Retapez le nouveau mot de passe" required className="form-input" />
            </div>
            {error   && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>}
            {success && <p className="text-green-600 text-sm bg-green-50 px-3 py-2 rounded-lg">{success}</p>}
            <button type="submit" disabled={saving} className="btn-primary w-full flex items-center justify-center gap-2 disabled:opacity-60">
              <Save size={15} /> {saving ? 'Modification...' : 'Modifier le Mot de Passe'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default AgentProfilPage;
