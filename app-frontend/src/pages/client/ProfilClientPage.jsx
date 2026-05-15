import React, { useState, useEffect } from 'react';
import { User, Lock, Save, Mail, Phone, MapPin, Briefcase } from 'lucide-react';
import { getCurrentUser } from '../../utils/auth';
import { getMyClientProfile } from '../../api/clientsApi';
import authApiClient from '../../api/authApiClient';

const val = (v) => v || '—';

const ProfilClientPage = () => {
  const user = getCurrentUser();

  const [activeTab, setActiveTab] = useState('infos');
  const [profile,   setProfile]   = useState(null);
  const [loading,   setLoading]   = useState(true);
  const [mdpForm,   setMdpForm]   = useState({ ancien: '', nouveau: '', confirmer: '' });
  const [error,     setError]     = useState('');
  const [success,   setSuccess]   = useState('');
  const [saving,    setSaving]    = useState(false);

  useEffect(() => {
    getMyClientProfile()
      .then(res => setProfile(res.data))
      .catch(() => setProfile(null))
      .finally(() => setLoading(false));
  }, []);

  const handleChangerMdp = async (e) => {
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

  const nom    = profile ? `${profile.firstName ?? profile.prenom ?? ''} ${profile.lastName ?? profile.nom ?? ''}`.trim() : `${user?.firstName ?? user?.prenom ?? ''} ${user?.lastName ?? user?.nom ?? ''}`.trim();

  return (
    <div className="p-6 space-y-5">
      <h1 className="text-2xl font-bold text-gray-800">Mon Profil</h1>

      <div className="flex gap-1 bg-gray-100 p-1 rounded-lg w-fit">
        {[['infos', 'Mes Informations'], ['securite', 'Sécurité']].map(([key, label]) => (
          <button key={key} onClick={() => setActiveTab(key)}
            className={`px-4 py-2 text-sm rounded-md font-medium transition-colors ${
              activeTab === key ? 'bg-white text-gray-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            }`}>
            {label}
          </button>
        ))}
      </div>

      {activeTab === 'infos' && (
        <div className="card">
          <div className="flex items-center gap-4 mb-6">
            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center">
              <User size={28} className="text-blue-600" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-800">{nom || '—'}</h2>
              <p className="text-gray-400 text-sm">Client MicroFinanceHub</p>
              {profile?.creditScore != null && (
                <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full">
                  Score de crédit : {profile.creditScore}
                </span>
              )}
            </div>
          </div>

          {loading ? (
            <p className="text-center text-gray-400 py-6">Chargement...</p>
          ) : (
            <div className="grid grid-cols-2 gap-x-8 gap-y-4 text-sm">
              <div>
                <p className="text-xs text-gray-400 mb-0.5">Prénom</p>
                <p className="font-medium text-gray-700">{val(profile?.firstName ?? user?.firstName ?? user?.prenom)}</p>
              </div>
              <div>
                <p className="text-xs text-gray-400 mb-0.5">Nom</p>
                <p className="font-medium text-gray-700">{val(profile?.lastName ?? user?.lastName ?? user?.nom)}</p>
              </div>
              <div className="flex items-start gap-2">
                <Mail size={13} className="text-gray-400 mt-0.5" />
                <div>
                  <p className="text-xs text-gray-400 mb-0.5">Email</p>
                  <p className="font-medium text-gray-700">{val(profile?.email ?? user?.email)}</p>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Phone size={13} className="text-gray-400 mt-0.5" />
                <div>
                  <p className="text-xs text-gray-400 mb-0.5">Téléphone</p>
                  <p className="font-medium text-gray-700">{val(profile?.phoneNumber)}</p>
                </div>
              </div>
              {profile?.address && (
                <div className="flex items-start gap-2 col-span-2">
                  <MapPin size={13} className="text-gray-400 mt-0.5" />
                  <div>
                    <p className="text-xs text-gray-400 mb-0.5">Adresse</p>
                    <p className="font-medium text-gray-700">
                      {profile.address}{profile.city ? `, ${profile.city}` : ''}
                    </p>
                  </div>
                </div>
              )}
              {profile?.profession && (
                <div className="flex items-start gap-2">
                  <Briefcase size={13} className="text-gray-400 mt-0.5" />
                  <div>
                    <p className="text-xs text-gray-400 mb-0.5">Profession</p>
                    <p className="font-medium text-gray-700">{profile.profession}</p>
                  </div>
                </div>
              )}
              {profile?.nationalId && (
                <div>
                  <p className="text-xs text-gray-400 mb-0.5">Pièce d'identité</p>
                  <p className="font-medium text-gray-700">{profile.nationalId}</p>
                </div>
              )}
              {profile?.status && (
                <div>
                  <p className="text-xs text-gray-400 mb-0.5">Statut</p>
                  <p className="font-medium text-gray-700">{profile.status}</p>
                </div>
              )}
            </div>
          )}

          <p className="text-xs text-gray-400 mt-4 bg-blue-50 p-3 rounded-lg">
            Pour modifier vos informations personnelles, veuillez contacter un agent ou vous rendre en agence.
          </p>
        </div>
      )}

      {activeTab === 'securite' && (
        <div className="card max-w-md">
          <h3 className="font-semibold text-gray-700 flex items-center gap-2 mb-5">
            <Lock size={16} className="text-blue-500" /> Changer mon Mot de Passe
          </h3>
          <form onSubmit={handleChangerMdp} className="space-y-4">
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
              <label className="form-label">Confirmer le Nouveau Mot de Passe *</label>
              <input type="password" value={mdpForm.confirmer}
                onChange={e => setMdpForm(p => ({ ...p, confirmer: e.target.value }))}
                placeholder="Retapez le nouveau mot de passe" required className="form-input" />
            </div>
            {error   && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>}
            {success && <p className="text-green-600 text-sm bg-green-50 px-3 py-2 rounded-lg">{success}</p>}
            <button type="submit" disabled={saving} className="btn-primary flex items-center gap-2 w-full justify-center disabled:opacity-60">
              <Save size={15} /> {saving ? 'Modification...' : 'Modifier le Mot de Passe'}
            </button>
          </form>
        </div>
      )}
    </div>
  );
};

export default ProfilClientPage;
