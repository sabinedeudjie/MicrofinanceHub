import React, { useState, useEffect } from 'react';
import { UserCheck, Users, TrendingUp, Activity, Plus, Building2, Pencil, Trash2, ToggleLeft, ToggleRight } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import { registerAgent, updateUser, deleteUser, toggleUser } from '../../api/authApi';
import { getAllAgencies, assignAgent, getAllActiveAssignments } from '../../api/agencyApi';
import authApiClient from '../../api/authApiClient';

const AgentsPage = () => {
  const [agents, setAgents]             = useState([]);
  const [agences, setAgences]           = useState([]);
  const [assignments, setAssignments]   = useState({});  // agentId → assignment
  const [loading, setLoading]           = useState(true);

  // Modals
  const [modalCreation, setModalCreation] = useState(false);
  const [modalAgence, setModalAgence]     = useState(false);
  const [modalEdit, setModalEdit]         = useState(false);
  const [modalSupp, setModalSupp]         = useState(false);

  // Selected
  const [agentSelecte, setAgentSelecte] = useState(null);
  const [agenceId, setAgenceId]         = useState('');
  const [confirmAssign, setConfirmAssign] = useState(false);

  // Forms
  const [newAgent, setNewAgent] = useState({ nom: '', prenom: '', email: '', telephone: '', motDePasse: '' });
  const [editForm, setEditForm] = useState({ firstName: '', lastName: '', phoneNumber: '' });

  const [erreur, setErreur]         = useState('');
  const [editErreur, setEditErreur] = useState('');
  const [saving, setSaving]         = useState(false);
  const [suppLoading, setSuppLoading] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const [agentRes, agenceRes, assignRes] = await Promise.allSettled([
        authApiClient.get('/auth/users/by-role/AGENT').catch(() => ({ data: [] })),
        getAllAgencies(),
        getAllActiveAssignments().catch(() => ({ data: [] })),
      ]);
      setAgents(agentRes.status === 'fulfilled' && Array.isArray(agentRes.value.data) ? agentRes.value.data : []);
      setAgences(agenceRes.status === 'fulfilled' ? (agenceRes.value.data ?? []) : []);
      const assignList = assignRes.status === 'fulfilled' ? (assignRes.value.data ?? []) : [];
      const map = {};
      assignList.forEach(a => { map[a.agentId] = a; });
      setAssignments(map);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  // ── Créer un agent ──────────────────────────────────────────────────────────
  const handleCreerAgent = async (e) => {
    e.preventDefault();
    setErreur('');
    setSaving(true);
    const result = await registerAgent(newAgent);
    if (!result.success) {
      setErreur(result.error);
      setSaving(false);
      return;
    }
    await loadData();
    setModalCreation(false);
    setNewAgent({ nom: '', prenom: '', email: '', telephone: '', motDePasse: '' });
    setSaving(false);
  };

  // ── Modifier un agent ───────────────────────────────────────────────────────
  const openEdit = (agent) => {
    setAgentSelecte(agent);
    setEditForm({ firstName: agent.firstName ?? '', lastName: agent.lastName ?? '', phoneNumber: agent.phoneNumber ?? '' });
    setEditErreur('');
    setModalEdit(true);
  };

  const handleEdit = async (e) => {
    e.preventDefault();
    if (!agentSelecte) return;
    setSaving(true);
    setEditErreur('');
    try {
      await updateUser(agentSelecte.id, editForm);
      await loadData();
      setModalEdit(false);
      setAgentSelecte(null);
    } catch (err) {
      setEditErreur(err.response?.data?.message ?? 'Erreur lors de la modification');
    } finally {
      setSaving(false);
    }
  };

  // ── Supprimer un agent ──────────────────────────────────────────────────────
  const openSupp = (agent) => {
    setAgentSelecte(agent);
    setModalSupp(true);
  };

  const handleSupprimer = async () => {
    if (!agentSelecte) return;
    setSuppLoading(true);
    try {
      await deleteUser(agentSelecte.id);
      await loadData();
      setModalSupp(false);
      setAgentSelecte(null);
    } catch (err) {
      setErreur(err.response?.data?.message ?? 'Erreur lors de la suppression');
      setModalSupp(false);
    } finally {
      setSuppLoading(false);
    }
  };

  // ── Toggle actif/inactif ────────────────────────────────────────────────────
  const handleToggle = async (agent) => {
    try {
      await toggleUser(agent.id);
      await loadData();
    } catch (err) {
      setErreur(err.response?.data?.message ?? 'Erreur lors du changement de statut');
    }
  };

  // ── Assigner à une agence ───────────────────────────────────────────────────
  const openModalAgence = (agent) => {
    setAgentSelecte(agent);
    setAgenceId('');
    setErreur('');
    setModalAgence(true);
  };

  const doAssignerAgence = async (aid, agId) => {
    setErreur('');
    setSaving(true);
    try {
      await assignAgent(agId, aid);
      await loadData();
      setModalAgence(false);
      setConfirmAssign(false);
      setAgentSelecte(null);
      setAgenceId('');
    } catch (err) {
      setErreur(err.response?.data?.message ?? "Erreur lors de l'assignation");
    } finally {
      setSaving(false);
    }
  };

  const handleAssignerAgence = async (e) => {
    e.preventDefault();
    if (!agentSelecte || !agenceId) return;
    const existingAssignment = assignments[agentSelecte.id];
    if (existingAssignment && existingAssignment.agencyId !== agenceId) {
      setConfirmAssign(true);
    } else {
      await doAssignerAgence(agentSelecte.id, agenceId);
    }
  };

  const actifs   = agents.filter(a => a.enabled).length;
  const inactifs = agents.filter(a => !a.enabled).length;

  return (
    <div className="p-6 space-y-6">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Gestion des Agents</h1>
          <p className="text-gray-500 text-sm">Gérez les agents de terrain et leurs portefeuilles clients</p>
        </div>
        <button onClick={() => setModalCreation(true)} className="btn-primary flex items-center gap-2">
          <Plus size={16} /> Nouvel Agent
        </button>
      </div>

      {erreur && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
          {erreur}
        </div>
      )}

      <div className="grid grid-cols-4 gap-4">
        <StatCard title="Total Agents"      value={String(agents.length)} icon={UserCheck}  iconBg="bg-blue-100"   iconColor="text-blue-600" />
        <StatCard title="Agents Actifs"     value={String(actifs)}        icon={Activity}   iconBg="bg-green-100"  iconColor="text-green-600" />
        <StatCard title="Agents Inactifs"   value={String(inactifs)}      icon={Users}      iconBg="bg-red-100"    iconColor="text-red-500" />
        <StatCard title="Collectes ce Mois" value="—"                     icon={TrendingUp} iconBg="bg-orange-100" iconColor="text-orange-600" />
      </div>

      <div className="card">
        <h3 className="font-semibold text-gray-700 mb-1">Liste des Agents</h3>
        <p className="text-xs text-gray-400 mb-4">
          {loading ? 'Chargement...' : agents.length === 0
            ? 'Aucun agent — utilisez "Nouvel Agent" pour en ajouter'
            : `${agents.length} agent(s)`}
        </p>
        <table className="w-full text-sm">
          <thead>
            <tr className="text-xs text-gray-400 border-b border-gray-100">
              <th className="text-left pb-3">Nom</th>
              <th className="text-left pb-3">Email</th>
              <th className="text-left pb-3">Téléphone</th>
              <th className="text-left pb-3">Agence</th>
              <th className="text-left pb-3">Statut</th>
              <th className="text-left pb-3">Créé le</th>
              <th className="text-left pb-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="py-8 text-center text-gray-400">Chargement des agents...</td></tr>
            ) : agents.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-12 text-center text-gray-300">
                  <UserCheck size={32} className="mx-auto mb-2" />
                  <p className="text-sm">Aucun agent — cliquez sur "Nouvel Agent" pour créer le premier</p>
                </td>
              </tr>
            ) : (
              agents.map(a => {
                const assignment = assignments[a.id];
                return (
                <tr key={a.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="py-3 font-medium text-gray-800">{a.firstName} {a.lastName}</td>
                  <td className="py-3 text-gray-500 text-xs">{a.email}</td>
                  <td className="py-3 text-gray-600">{a.phoneNumber || '—'}</td>
                  <td className="py-3">
                    {assignment ? (
                      <span className="inline-flex items-center gap-1 text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded-full font-medium"
                        title={assignment.agencyName ?? ''}>
                        <Building2 size={11} />
                        {assignment.agencyCode}
                      </span>
                    ) : (
                      <span className="text-xs text-gray-400 italic">Non assigné</span>
                    )}
                  </td>
                  <td className="py-3"><Badge status={a.enabled ? 'actif' : 'inactif'} /></td>
                  <td className="py-3 text-xs text-gray-400">
                    {a.createdAt ? new Date(a.createdAt).toLocaleDateString('fr-FR') : '—'}
                  </td>
                  <td className="py-3">
                    <div className="flex items-center gap-1">
                      <button onClick={() => openEdit(a)} title="Modifier"
                        className="p-1.5 hover:bg-yellow-50 text-gray-400 hover:text-yellow-600 rounded-lg">
                        <Pencil size={15} />
                      </button>
                      <button onClick={() => handleToggle(a)}
                        title={a.enabled ? 'Désactiver' : 'Activer'}
                        className="p-1.5 hover:bg-orange-50 text-gray-400 hover:text-orange-500 rounded-lg">
                        {a.enabled
                          ? <ToggleRight size={15} className="text-green-500" />
                          : <ToggleLeft size={15} className="text-red-400" />}
                      </button>
                      <button onClick={() => openModalAgence(a)} title="Assigner à une agence"
                        className="p-1.5 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded-lg">
                        <Building2 size={15} />
                      </button>
                      <button onClick={() => openSupp(a)} title="Supprimer"
                        className="p-1.5 hover:bg-red-50 text-gray-400 hover:text-red-500 rounded-lg">
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </td>
                </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* ── Modal Créer ────────────────────────────────────────────────────── */}
      <Modal isOpen={modalCreation} onClose={() => { setModalCreation(false); setErreur(''); }}
        title="Créer un Compte Agent" subtitle="Renseignez les informations du nouvel agent">
        <form onSubmit={handleCreerAgent} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Nom *</label>
              <input value={newAgent.nom} onChange={e => setNewAgent(p => ({...p, nom: e.target.value}))}
                placeholder="Ex: Kamga" required className="form-input" />
            </div>
            <div>
              <label className="form-label">Prénom *</label>
              <input value={newAgent.prenom} onChange={e => setNewAgent(p => ({...p, prenom: e.target.value}))}
                placeholder="Ex: Bertrand" required className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Email *</label>
            <input type="email" value={newAgent.email} onChange={e => setNewAgent(p => ({...p, email: e.target.value}))}
              placeholder="prenom.nom@mfh.cm" required className="form-input" />
          </div>
          <div>
            <label className="form-label">Téléphone</label>
            <input value={newAgent.telephone} onChange={e => setNewAgent(p => ({...p, telephone: e.target.value}))}
              placeholder="+237 6XX XXX XXX" className="form-input" />
          </div>
          <div>
            <label className="form-label">Mot de Passe Provisoire *</label>
            <input type="password" value={newAgent.motDePasse}
              onChange={e => setNewAgent(p => ({...p, motDePasse: e.target.value}))}
              placeholder="Minimum 6 caractères" required minLength={6} className="form-input" />
          </div>
          {erreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{erreur}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalCreation(false); setErreur(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={saving} className="btn-primary">{saving ? 'Création...' : "Créer l'Agent"}</button>
          </div>
        </form>
      </Modal>

      {/* ── Modal Modifier ─────────────────────────────────────────────────── */}
      <Modal isOpen={modalEdit} onClose={() => { setModalEdit(false); setEditErreur(''); }}
        title="Modifier l'Agent"
        subtitle={agentSelecte ? `${agentSelecte.firstName} ${agentSelecte.lastName}` : ''}>
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
            <label className="form-label">Téléphone</label>
            <input value={editForm.phoneNumber} onChange={e => setEditForm(p => ({ ...p, phoneNumber: e.target.value }))}
              placeholder="+237 6XX XXX XXX" className="form-input" />
          </div>
          {editErreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{editErreur}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalEdit(false); setEditErreur(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={saving} className="btn-primary disabled:opacity-60">
              {saving ? 'Enregistrement...' : 'Enregistrer'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ── Modal Assigner Agence ──────────────────────────────────────────── */}
      <Modal isOpen={modalAgence} onClose={() => { setModalAgence(false); setErreur(''); setConfirmAssign(false); }}
        title="Assigner à une Agence"
        subtitle={agentSelecte ? `${agentSelecte.firstName} ${agentSelecte.lastName}` : ''}>

        {confirmAssign ? (
          <div className="space-y-4">
            <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-sm text-amber-800">
              <p className="font-semibold mb-1">Attention</p>
              <p>
                Cet agent appartient déjà à l'agence{' '}
                <strong>{agentSelecte && assignments[agentSelecte.id]?.agencyName} ({agentSelecte && assignments[agentSelecte.id]?.agencyCode})</strong>.
                Êtes-vous sûr de vouloir l'assigner à une autre agence ?
              </p>
            </div>
            {erreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{erreur}</p>}
            <div className="flex justify-end gap-3">
              <button onClick={() => setConfirmAssign(false)} className="btn-secondary">Non, annuler</button>
              <button onClick={() => doAssignerAgence(agentSelecte.id, agenceId)} disabled={saving}
                className="btn-primary disabled:opacity-60">
                {saving ? 'Assignation...' : 'Oui, changer l\'agence'}
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleAssignerAgence} className="space-y-4">
            <div>
              <label className="form-label">Agence *</label>
              <select value={agenceId} onChange={e => setAgenceId(e.target.value)} required className="form-input">
                <option value="">— Choisir une agence —</option>
                {agences.map(ag => (
                  <option key={ag.id} value={ag.id}>{ag.code} — {ag.name} ({ag.city ?? '—'})</option>
                ))}
              </select>
              {agences.length === 0 && (
                <p className="text-xs text-amber-600 mt-1">Aucune agence disponible.</p>
              )}
            </div>
            {erreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{erreur}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => { setModalAgence(false); setErreur(''); }} className="btn-secondary">Annuler</button>
              <button type="submit" disabled={saving || !agenceId} className="btn-primary disabled:opacity-60">
                {saving ? 'Assignation...' : agentSelecte && assignments[agentSelecte.id] ? "Changer l'agence" : 'Assigner'}
              </button>
            </div>
          </form>
        )}
      </Modal>

      {/* ── Modal Suppression ─────────────────────────────────────────────── */}
      <Modal isOpen={modalSupp} onClose={() => setModalSupp(false)}
        title="Supprimer l'Agent"
        subtitle={agentSelecte ? `${agentSelecte.firstName} ${agentSelecte.lastName}` : ''}>
        <div className="space-y-4">
          <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-700">
            <p className="font-semibold mb-1">Cette action est irréversible.</p>
            <p>Le compte de cet agent sera définitivement supprimé du système.</p>
          </div>
          <p className="text-sm text-gray-600">
            Confirmez-vous la suppression de <strong>{agentSelecte?.firstName} {agentSelecte?.lastName}</strong> ?
          </p>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalSupp(false)} className="btn-secondary">Annuler</button>
            <button onClick={handleSupprimer} disabled={suppLoading}
              className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg text-sm font-medium disabled:opacity-60 flex items-center gap-2">
              <Trash2 size={14} /> {suppLoading ? 'Suppression...' : 'Confirmer'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default AgentsPage;
