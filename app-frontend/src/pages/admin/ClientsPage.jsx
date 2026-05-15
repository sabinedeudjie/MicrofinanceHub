import React, { useState, useEffect } from 'react';
import { Users, UserCheck, UserX, UserPlus, Search, Eye, Pencil, UserCog, RefreshCw } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import { getAllClients, getClientStats, createClient, updateClient, deleteClient, updateClientStatus, assignClientToAgent } from '../../api/clientsApi';
import { getAllActiveAssignments } from '../../api/agencyApi';
import { useNavigate } from 'react-router-dom';

const EMPTY_CREATE = {
  prenom: '', nom: '', email: '', telephone: '',
  adresse: '', dateNaissance: '', clientType: 'INDIVIDUAL',
};

const ClientsPage = () => {
  const navigate = useNavigate();
  const [clients, setClients]   = useState([]);
  const [stats, setStats]       = useState(null);
  const [search, setSearch]     = useState('');
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState('');

  // Modal création
  const [modalCreate, setModalCreate]   = useState(false);
  const [createForm, setCreateForm]     = useState(EMPTY_CREATE);
  const [createSaving, setCreateSaving] = useState(false);
  const [createError, setCreateError]   = useState('');

  // Modal édition
  const [modalEdit, setModalEdit]     = useState(false);
  const [clientEdit, setClientEdit]   = useState(null);
  const [editForm, setEditForm]       = useState({});
  const [editSaving, setEditSaving]   = useState(false);
  const [editErreur, setEditErreur]   = useState('');

  // Modal suppression
  const [modalSupp, setModalSupp]     = useState(false);
  const [clientSupp, setClientSupp]   = useState(null);
  const [suppLoading, setSuppLoading] = useState(false);

  // Modal assignation agent
  const [modalAssign, setModalAssign]       = useState(false);
  const [clientAssign, setClientAssign]     = useState(null);
  const [assignments, setAssignments]       = useState([]);
  const [selectedAgentId, setSelectedAgentId] = useState('');
  const [assignSaving, setAssignSaving]     = useState(false);
  const [assignError, setAssignError]       = useState('');

  const handleCreer = async (e) => {
    e.preventDefault();
    setCreateSaving(true);
    setCreateError('');
    try {
      await createClient({
        email:       createForm.email.trim().toLowerCase(),
        phoneNumber: createForm.telephone?.replace(/\s/g, '') || null,
        firstName:   createForm.prenom,
        lastName:    createForm.nom,
        address:     createForm.adresse || '',
        birthDate:   createForm.dateNaissance ? createForm.dateNaissance + 'T00:00:00' : null,
        clientType:  createForm.clientType,
      });
      await loadData();
      setModalCreate(false);
      setCreateForm(EMPTY_CREATE);
    } catch (err) {
      const data = err.response?.data;
      if (data?.validationErrors && Object.keys(data.validationErrors).length > 0) {
        setCreateError(Object.values(data.validationErrors).join(' • '));
      } else {
        setCreateError(data?.message ?? 'Erreur lors de la création.');
      }
    } finally {
      setCreateSaving(false);
    }
  };

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [cr, sr, ar] = await Promise.allSettled([getAllClients(), getClientStats(), getAllActiveAssignments()]);
      if (cr.status === 'fulfilled') setClients(cr.value.data || []);
      if (sr.status === 'fulfilled') setStats(sr.value.data);
      if (ar.status === 'fulfilled') setAssignments(ar.value.data || []);
      if (cr.status === 'rejected')  setError('Impossible de charger les clients.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  const filtered = clients.filter(c => {
    const q = search.toLowerCase();
    return (
      c.firstName?.toLowerCase().includes(q) ||
      c.lastName?.toLowerCase().includes(q) ||
      c.email?.toLowerCase().includes(q) ||
      c.id?.toLowerCase().includes(q)
    );
  });

  // ── Ouvrir modal édition ────────────────────────────────────────────────────
  const openEdit = (client) => {
    setClientEdit(client);
    setEditForm({
      firstName:  client.firstName  ?? '',
      lastName:   client.lastName   ?? '',
      email:      client.email      ?? '',
      phoneNumber: client.phoneNumber ?? '',
      address:    client.address    ?? '',
      clientType: client.clientType ?? 'INDIVIDUAL',
    });
    setEditErreur('');
    setModalEdit(true);
  };

  const handleEdit = async (e) => {
    e.preventDefault();
    if (!clientEdit) return;
    setEditSaving(true);
    setEditErreur('');
    try {
      await updateClient(clientEdit.id, {
        ...editForm,
        phoneNumber: editForm.phoneNumber?.replace(/\s/g, '') || null,
      });
      await loadData();
      setModalEdit(false);
      setClientEdit(null);
    } catch (err) {
      const data = err.response?.data;
      if (data?.validationErrors && Object.keys(data.validationErrors).length > 0) {
        setEditErreur(Object.values(data.validationErrors).join(' • '));
      } else {
        setEditErreur(data?.message ?? 'Erreur lors de la modification');
      }
    } finally {
      setEditSaving(false);
    }
  };

  // ── Ouvrir modal suppression ────────────────────────────────────────────────
  const openSupp = (client) => {
    setClientSupp(client);
    setModalSupp(true);
  };

  const handleDesactiver = async () => {
    if (!clientSupp) return;
    setSuppLoading(true);
    try {
      await deleteClient(clientSupp.id);
      await loadData();
      setModalSupp(false);
      setClientSupp(null);
    } catch (err) {
      setError(err.response?.data?.message ?? 'Erreur lors de la désactivation');
      setModalSupp(false);
    } finally {
      setSuppLoading(false);
    }
  };

  const handleReactiver = async (client) => {
    try {
      await updateClientStatus(client.id, 'ACTIVE');
      await loadData();
    } catch (err) {
      setError(err.response?.data?.message ?? 'Erreur lors de la réactivation');
    }
  };

  const openAssign = (client) => {
    setClientAssign(client);
    setSelectedAgentId('');
    setAssignError('');
    setModalAssign(true);
  };

  const handleAssign = async (e) => {
    e.preventDefault();
    if (!selectedAgentId || !clientAssign) return;
    const agent = assignments.find(a => a.agentId === selectedAgentId);
    if (!agent) return;
    setAssignSaving(true);
    setAssignError('');
    try {
      await assignClientToAgent(clientAssign.id, agent.agentEmail, agent.agencyId);
      await loadData();
      setModalAssign(false);
      setClientAssign(null);
    } catch (err) {
      setAssignError(err.response?.data?.message ?? "Erreur lors de l'assignation.");
    } finally {
      setAssignSaving(false);
    }
  };

  return (
    <div className="p-6 space-y-6">

      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Gestion des Clients</h1>
          <p className="text-gray-500 text-sm">Gérez les profils et l'historique de vos clients</p>
        </div>
        <button onClick={() => { setCreateForm(EMPTY_CREATE); setCreateError(''); setModalCreate(true); }}
          className="btn-primary flex items-center gap-2 w-full sm:w-auto justify-center">
          <UserPlus size={16} /> Nouveau Client
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Total Clients"    value={stats ? String(stats.totalClients)       : '—'} icon={Users}    iconBg="bg-blue-100"   iconColor="text-blue-600" />
        <StatCard title="Clients Actifs"   value={stats ? String(stats.activeClients)      : '—'} icon={UserCheck} iconBg="bg-green-100"  iconColor="text-green-600" />
        <StatCard title="Clients Inactifs" value={stats ? String(stats.inactiveClients)    : '—'} icon={UserX}    iconBg="bg-red-100"    iconColor="text-red-500" />
        <StatCard title="Ce mois"          value={stats ? String(stats.newClientsThisMonth): '—'} icon={UserPlus} iconBg="bg-orange-100" iconColor="text-orange-500" />
      </div>

      <div className="card">
        <div className="flex flex-col md:flex-row md:items-center justify-between mb-4 gap-4">
          <div>
            <h3 className="font-semibold text-gray-700">Liste des Clients</h3>
            <p className="text-xs text-gray-400">
              {loading ? 'Chargement...' : `${filtered.length} client(s) trouvé(s)`}
            </p>
          </div>
          <div className="relative w-full md:w-64">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input value={search} onChange={e => setSearch(e.target.value)}
              placeholder="Rechercher..." className="form-input pl-9 w-full text-sm" />
          </div>
        </div>

        {error && <p className="text-red-500 text-sm mb-4 bg-red-50 px-3 py-2 rounded-lg">{error}</p>}

        <div className="table-container">
          <table className="w-full text-sm min-w-[800px]">
            <thead>
              <tr className="text-xs text-gray-400 border-b border-gray-100">
                <th className="text-left pb-3">Nom</th>
                <th className="text-left pb-3">Email</th>
                <th className="text-left pb-3">Téléphone</th>
                <th className="text-left pb-3">Type</th>
                <th className="text-left pb-3">Statut</th>
                <th className="text-left pb-3">Score</th>
                <th className="text-left pb-3">Inscrit le</th>
                <th className="text-left pb-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={8} className="py-8 text-center text-gray-400">Chargement des clients...</td></tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-gray-300">
                    <Users size={32} className="mx-auto mb-2" />
                    <p className="text-sm">
                      {clients.length === 0
                        ? 'Aucun client — cliquez sur "Nouveau Client" pour inscrire le premier'
                        : 'Aucun résultat pour cette recherche'}
                    </p>
                  </td>
                </tr>
              ) : (
                filtered.map(client => (
                  <tr key={client.id} className="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                    <td className="py-3 font-medium text-gray-800">{client.firstName} {client.lastName}</td>
                    <td className="py-3 text-gray-500 text-xs">{client.email}</td>
                    <td className="py-3 text-gray-600">{client.phoneNumber || '—'}</td>
                    <td className="py-3 text-gray-500 text-xs">{client.clientType || '—'}</td>
                    <td className="py-3"><Badge status={client.status?.toLowerCase() || 'actif'} /></td>
                    <td className="py-3 text-gray-600">{client.creditScore ?? '—'}</td>
                    <td className="py-3 text-xs text-gray-400">
                      {client.createdAt ? new Date(client.createdAt).toLocaleDateString('fr-FR') : '—'}
                    </td>
                    <td className="py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          onClick={() => navigate(`/admin/clients/${client.id}`)}
                          title="Voir le profil"
                          className="p-1.5 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded-lg"
                        >
                          <Eye size={15} />
                        </button>
                        <button
                          onClick={() => openEdit(client)}
                          title="Modifier"
                          className="p-1.5 hover:bg-yellow-50 text-gray-400 hover:text-yellow-600 rounded-lg"
                        >
                          <Pencil size={15} />
                        </button>
                        <button
                          onClick={() => openAssign(client)}
                          title="Assigner à un agent"
                          className="p-1.5 hover:bg-indigo-50 text-gray-400 hover:text-indigo-600 rounded-lg"
                        >
                          <UserCog size={15} />
                        </button>
                        {client.status === 'INACTIVE' ? (
                          <button
                            onClick={() => handleReactiver(client)}
                            title="Réactiver le client"
                            className="p-1.5 hover:bg-green-50 text-gray-400 hover:text-green-600 rounded-lg"
                          >
                            <RefreshCw size={15} />
                          </button>
                        ) : (
                          <button
                            onClick={() => openSupp(client)}
                            title="Désactiver le client"
                            className="p-1.5 hover:bg-orange-50 text-gray-400 hover:text-orange-500 rounded-lg"
                          >
                            <UserX size={15} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Modal Création ──────────────────────────────────────────────────── */}
      <Modal isOpen={modalCreate} onClose={() => { setModalCreate(false); setCreateError(''); }}
        title="Enregistrer un Client"
        subtitle="Créez le profil KYC — le client créera lui-même son mot de passe">
        <form onSubmit={handleCreer} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Prénom *</label>
              <input value={createForm.prenom} onChange={e => setCreateForm(p => ({ ...p, prenom: e.target.value }))}
                placeholder="Ex: Marie" required className="form-input" />
            </div>
            <div>
              <label className="form-label">Nom *</label>
              <input value={createForm.nom} onChange={e => setCreateForm(p => ({ ...p, nom: e.target.value }))}
                placeholder="Ex: Kouam" required className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Email *</label>
            <input type="email" value={createForm.email} onChange={e => setCreateForm(p => ({ ...p, email: e.target.value }))}
              placeholder="client@email.cm" required className="form-input" />
            <p className="text-xs text-gray-400 mt-1">Le client utilisera cet email pour s'inscrire sur la plateforme.</p>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Téléphone *</label>
              <input value={createForm.telephone} onChange={e => setCreateForm(p => ({ ...p, telephone: e.target.value }))}
                placeholder="+237600000000" required className="form-input" />
            </div>
            <div>
              <label className="form-label">Date de Naissance</label>
              <input type="date" value={createForm.dateNaissance} onChange={e => setCreateForm(p => ({ ...p, dateNaissance: e.target.value }))}
                className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Adresse</label>
            <input value={createForm.adresse} onChange={e => setCreateForm(p => ({ ...p, adresse: e.target.value }))}
              placeholder="Ex: Akwa, Douala" className="form-input" />
          </div>
          <div>
            <label className="form-label">Type de Client</label>
            <select value={createForm.clientType} onChange={e => setCreateForm(p => ({ ...p, clientType: e.target.value }))}
              className="form-input">
              <option value="INDIVIDUAL">Individuel</option>
              <option value="BUSINESS">Entreprise</option>
            </select>
          </div>
          {createError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{createError}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalCreate(false); setCreateError(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={createSaving} className="btn-primary disabled:opacity-60">
              {createSaving ? 'Création...' : 'Créer le Profil'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ── Modal Édition ───────────────────────────────────────────────────── */}
      <Modal isOpen={modalEdit} onClose={() => { setModalEdit(false); setEditErreur(''); }}
        title="Modifier le Client"
        subtitle={clientEdit ? `${clientEdit.firstName} ${clientEdit.lastName}` : ''}>
        <form onSubmit={handleEdit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Prénom *</label>
              <input value={editForm.firstName} onChange={e => setEditForm(p => ({ ...p, firstName: e.target.value }))}
                required className="form-input" />
            </div>
            <div>
              <label className="form-label">Nom *</label>
              <input value={editForm.lastName} onChange={e => setEditForm(p => ({ ...p, lastName: e.target.value }))}
                required className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Email *</label>
            <input type="email" value={editForm.email} onChange={e => setEditForm(p => ({ ...p, email: e.target.value }))}
              required className="form-input" />
          </div>
          <div>
            <label className="form-label">Téléphone</label>
            <input value={editForm.phoneNumber} onChange={e => setEditForm(p => ({ ...p, phoneNumber: e.target.value }))}
              className="form-input" />
          </div>
          <div>
            <label className="form-label">Adresse</label>
            <input value={editForm.address} onChange={e => setEditForm(p => ({ ...p, address: e.target.value }))}
              className="form-input" />
          </div>
          <div>
            <label className="form-label">Type de Client</label>
            <select value={editForm.clientType} onChange={e => setEditForm(p => ({ ...p, clientType: e.target.value }))}
              className="form-input">
              <option value="INDIVIDUAL">Individuel</option>
              <option value="BUSINESS">Entreprise</option>
            </select>
          </div>
          {editErreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{editErreur}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalEdit(false); setEditErreur(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={editSaving} className="btn-primary disabled:opacity-60">
              {editSaving ? 'Enregistrement...' : 'Enregistrer'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ── Modal Assignation Agent ─────────────────────────────────────────── */}
      <Modal isOpen={modalAssign} onClose={() => { setModalAssign(false); setAssignError(''); }}
        title="Assigner à un Agent"
        subtitle={clientAssign ? `${clientAssign.firstName} ${clientAssign.lastName}` : ''}>
        {(() => {
          const currentAgent = assignments.find(a => a.agentEmail === clientAssign?.createdBy);
          const isAlreadyAssigned = !!(clientAssign?.agencyId || clientAssign?.createdBy);
          return (
            <form onSubmit={handleAssign} className="space-y-4">
              {isAlreadyAssigned && (
                <div className="bg-amber-50 border border-amber-200 rounded-xl px-3 py-2.5 text-sm text-amber-800">
                  <p className="font-semibold mb-0.5">Ce client est déjà assigné à un agent.</p>
                  {currentAgent ? (
                    <p className="text-xs">Agent actuel : <strong>{currentAgent.agentName}</strong> ({currentAgent.agentEmail}) — {currentAgent.agencyName}</p>
                  ) : clientAssign?.createdBy ? (
                    <p className="text-xs">Agent actuel : <strong>{clientAssign.createdBy}</strong></p>
                  ) : null}
                  <p className="text-xs mt-1">Sélectionnez un nouvel agent ci-dessous pour changer l'assignation.</p>
                </div>
              )}
              <div>
                <label className="form-label">Agent *</label>
                <select value={selectedAgentId} onChange={e => setSelectedAgentId(e.target.value)}
                  required className="form-input">
                  <option value="">— Choisir un agent —</option>
                  {assignments
                    .filter(a => !clientAssign?.createdBy || a.agentEmail !== clientAssign.createdBy)
                    .map(a => (
                      <option key={a.agentId} value={a.agentId}>
                        {a.agentName} ({a.agentEmail}) — {a.agencyCode} {a.agencyName}
                      </option>
                    ))}
                </select>
                <p className="text-xs text-gray-400 mt-1">
                  Le client sera rattaché à l'agence de l'agent sélectionné.
                </p>
              </div>
              {assignError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{assignError}</p>}
              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={() => { setModalAssign(false); setAssignError(''); }} className="btn-secondary">Annuler</button>
                <button type="submit" disabled={assignSaving || !selectedAgentId} className="btn-primary disabled:opacity-60">
                  {assignSaving ? 'Assignation…' : isAlreadyAssigned ? "Changer l'agent" : 'Assigner'}
                </button>
              </div>
            </form>
          );
        })()}
      </Modal>

      {/* ── Modal Suppression ───────────────────────────────────────────────── */}
      <Modal isOpen={modalSupp} onClose={() => setModalSupp(false)}
        title="Désactiver le Client"
        subtitle={clientSupp ? `${clientSupp.firstName} ${clientSupp.lastName}` : ''}>
        <div className="space-y-4">
          <div className="bg-orange-50 border border-orange-200 rounded-xl p-4 text-sm text-orange-800">
            <p className="font-semibold mb-1">Le client sera désactivé (statut INACTIF).</p>
            <p>Ses comptes, prêts et transactions sont conservés intégralement. Vous pouvez le réactiver à tout moment.</p>
          </div>
          <p className="text-sm text-gray-600">
            Confirmez-vous la désactivation de <strong>{clientSupp?.firstName} {clientSupp?.lastName}</strong> ?
          </p>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalSupp(false)} className="btn-secondary">Annuler</button>
            <button onClick={handleDesactiver} disabled={suppLoading}
              className="bg-orange-500 hover:bg-orange-600 text-white px-4 py-2 rounded-lg text-sm font-medium disabled:opacity-60 flex items-center gap-2">
              <UserX size={14} /> {suppLoading ? 'Désactivation...' : 'Désactiver'}
            </button>
          </div>
        </div>
      </Modal>

    </div>
  );
};

export default ClientsPage;
