import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserPlus, Search, Eye, Users } from 'lucide-react';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import { getMyClients, createClient } from '../../api/clientsApi';
import { getCurrentUser } from '../../utils/auth';

const EMPTY_FORM = {
  prenom: '', nom: '', email: '', telephone: '',
  adresse: '', dateNaissance: '', clientType: 'INDIVIDUAL',
};

const AgentClientsPage = () => {
  const navigate = useNavigate();
  const [search, setSearch]     = useState('');
  const [clients, setClients]   = useState([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState('');

  const [modalCreation, setModalCreation] = useState(false);
  const [form, setForm]                   = useState(EMPTY_FORM);
  const [saving, setSaving]               = useState(false);
  const [createError, setCreateError]     = useState('');
  const [successMsg, setSuccessMsg]       = useState('');

  const loadClients = () => {
    setLoading(true);
    getMyClients()
      .then(res => setClients(res.data || []))
      .catch(() => setError('Impossible de charger vos clients.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadClients(); }, []);

  const filtered = clients.filter(c => {
    const q = search.toLowerCase();
    return (
      c.firstName?.toLowerCase().includes(q) ||
      c.lastName?.toLowerCase().includes(q) ||
      c.email?.toLowerCase().includes(q)
    );
  });

  const openCreation = () => {
    setForm(EMPTY_FORM);
    setCreateError('');
    setSuccessMsg('');
    setModalCreation(true);
  };

  const handleCreer = async (e) => {
    e.preventDefault();
    setSaving(true);
    setCreateError('');
    const currentUser = getCurrentUser();
    try {
      await createClient({
        email:       form.email.trim().toLowerCase(),
        phoneNumber: form.telephone?.replace(/\s/g, '') || null,
        firstName:   form.prenom,
        lastName:    form.nom,
        address:     form.adresse || '',
        birthDate:   form.dateNaissance ? form.dateNaissance + 'T00:00:00' : null,
        clientType:  form.clientType,
        agencyId:    currentUser?.agencyId || null,
      });
      loadClients();
      setForm(EMPTY_FORM);
      setSuccessMsg(`Profil créé pour ${form.prenom} ${form.nom}. Le client peut maintenant s'inscrire avec l'adresse ${form.email.trim().toLowerCase()}.`);
    } catch (err) {
      const data = err.response?.data;
      if (data?.validationErrors && Object.keys(data.validationErrors).length > 0) {
        setCreateError(Object.values(data.validationErrors).join(' • '));
      } else {
        setCreateError(data?.message ?? 'Erreur lors de la création du client.');
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="p-6 space-y-5">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Mes Clients</h1>
          <p className="text-gray-500 text-sm">
            {loading ? 'Chargement...' : clients.length > 0
              ? `${clients.length} client(s) dans votre portefeuille`
              : 'Gérez votre portefeuille de clients'}
          </p>
        </div>
        <button onClick={openCreation} className="btn-primary flex items-center gap-2">
          <UserPlus size={16} /> Nouveau Client
        </button>
      </div>

      {error && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>}

      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-gray-700">Liste de mes Clients</h3>
          <div className="relative">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input value={search} onChange={e => setSearch(e.target.value)}
              placeholder="Rechercher..." className="form-input pl-9 w-60" />
          </div>
        </div>

        <div className="table-container">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-400 border-b border-gray-100">
                <th className="text-left pb-3">Nom</th>
                <th className="text-left pb-3">Email</th>
                <th className="text-left pb-3">Téléphone</th>
                <th className="text-left pb-3">Statut</th>
                <th className="text-left pb-3">Score</th>
                <th className="text-left pb-3">Inscrit le</th>
                <th className="text-left pb-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={7} className="py-8 text-center text-gray-400">Chargement...</td></tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-gray-300">
                    <Users size={32} className="mx-auto mb-2" />
                    <p className="text-sm">
                      {clients.length === 0
                        ? 'Aucun client assigné — utilisez "Nouveau Client" pour en inscrire un'
                        : 'Aucun résultat pour cette recherche'}
                    </p>
                  </td>
                </tr>
              ) : (
                filtered.map(c => (
                  <tr key={c.id} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 font-medium text-gray-800">{c.firstName} {c.lastName}</td>
                    <td className="py-3 text-gray-500 text-xs">{c.email}</td>
                    <td className="py-3 text-gray-600">{c.phoneNumber || '—'}</td>
                    <td className="py-3"><Badge status={c.status?.toLowerCase() || 'actif'} /></td>
                    <td className="py-3 text-gray-600">{c.creditScore ?? '—'}</td>
                    <td className="py-3 text-xs text-gray-400">
                      {c.createdAt ? new Date(c.createdAt).toLocaleDateString('fr-FR') : '—'}
                    </td>
                    <td className="py-3">
                      <button onClick={() => navigate(`/agent/clients/${c.id}`)}
                        className="p-1.5 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded-lg"
                        title="Voir le détail">
                        <Eye size={15} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Modal Création Client ─────────────────────────────────────────── */}
      <Modal isOpen={modalCreation} onClose={() => { setModalCreation(false); setSuccessMsg(''); }}
        title="Enregistrer un Client"
        subtitle="Créez le profil KYC — le client créera lui-même son mot de passe">

        {successMsg ? (
          <div className="space-y-4">
            <div className="bg-green-50 border border-green-200 rounded-xl p-4 text-sm text-green-700">
              <p className="font-semibold mb-1">Profil créé avec succès !</p>
              <p>{successMsg}</p>
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button onClick={() => { setModalCreation(false); setSuccessMsg(''); }} className="btn-primary">
                Fermer
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleCreer} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="form-label">Prénom *</label>
                <input value={form.prenom} onChange={e => setForm(p => ({ ...p, prenom: e.target.value }))}
                  placeholder="Ex: Marie" required className="form-input" />
              </div>
              <div>
                <label className="form-label">Nom *</label>
                <input value={form.nom} onChange={e => setForm(p => ({ ...p, nom: e.target.value }))}
                  placeholder="Ex: Kouam" required className="form-input" />
              </div>
            </div>
            <div>
              <label className="form-label">Email *</label>
              <input type="email" value={form.email} onChange={e => setForm(p => ({ ...p, email: e.target.value }))}
                placeholder="client@email.cm" required className="form-input" />
              <p className="text-xs text-gray-400 mt-1">
                Le client utilisera cet email pour s'inscrire sur la plateforme.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="form-label">Téléphone *</label>
                <input value={form.telephone} onChange={e => setForm(p => ({ ...p, telephone: e.target.value }))}
                  placeholder="+237600000000" required className="form-input" />
              </div>
              <div>
                <label className="form-label">Date de Naissance *</label>
                <input type="date" value={form.dateNaissance} onChange={e => setForm(p => ({ ...p, dateNaissance: e.target.value }))}
                  required className="form-input" />
              </div>
            </div>
            <div>
              <label className="form-label">Adresse *</label>
              <input value={form.adresse} onChange={e => setForm(p => ({ ...p, adresse: e.target.value }))}
                placeholder="Ex: Akwa, Douala" required className="form-input" />
            </div>
            <div>
              <label className="form-label">Type de Client</label>
              <select value={form.clientType} onChange={e => setForm(p => ({ ...p, clientType: e.target.value }))}
                className="form-input">
                <option value="INDIVIDUAL">Individuel</option>
                <option value="BUSINESS">Entreprise</option>
              </select>
            </div>
            {createError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{createError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setModalCreation(false)} className="btn-secondary">Annuler</button>
              <button type="submit" disabled={saving} className="btn-primary disabled:opacity-60">
                {saving ? 'Création...' : 'Créer le Profil'}
              </button>
            </div>
          </form>
        )}
      </Modal>

    </div>
  );
};

export default AgentClientsPage;
