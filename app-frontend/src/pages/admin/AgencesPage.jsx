import React, { useState, useEffect } from 'react';
import { Building2, Users, UserCheck, MapPin, Plus, ToggleLeft, ToggleRight, UserCog, UserPlus, UserX } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import { getAllAgencies, createAgency, assignDirecteur, toggleAgencyStatus, assignAgent, getAgencyAgents, getAllActiveAssignments, unassignAgentAdmin, unassignDirectorAdmin } from '../../api/agencyApi';
import { registerDirecteur } from '../../api/authApi';
import authApiClient from '../../api/authApiClient';

const EMPTY_DIR_FORM = { nom: '', prenom: '', email: '', telephone: '', motDePasse: '' };
const EMPTY_AGENCY_FORM = { code: '', name: '', city: '', address: '', region: '', phoneNumber: '', email: '', directorId: '' };

const AgencesPage = () => {
  const [agences, setAgences]               = useState([]);
  const [directeurs, setDirecteurs]         = useState([]);
  const [agents, setAgents]                 = useState([]);
  const [agentAssignments, setAgentAssignments] = useState({}); // agentId → assignment
  const [loading, setLoading]               = useState(true);

  // Modals
  const [modalCreation, setModalCreation]           = useState(false);
  const [modalDirecteur, setModalDirecteur]         = useState(false);
  const [modalNouveauDir, setModalNouveauDir]       = useState(false);
  const [modalAgent, setModalAgent]                 = useState(false);

  // Selected agency (for modals requiring an agency context)
  const [agenceSelectee, setAgenceSelectee]         = useState(null);
  const [confirmAgentId, setConfirmAgentId]         = useState(null); // agentId en attente de confirmation

  // Form states
  const [form, setForm]                             = useState(EMPTY_AGENCY_FORM);
  const [directeurId, setDirecteurId]               = useState('');
  const [agentId, setAgentId]                       = useState('');
  const [newDir, setNewDir]                         = useState(EMPTY_DIR_FORM);

  const [saving, setSaving]                         = useState(false);
  const [removingAgent, setRemovingAgent]           = useState(null); // agentId being removed
  const [removingDirector, setRemovingDirector]     = useState(false);
  const [erreur, setErreur]                         = useState('');
  const [toggleErreur, setToggleErreur]             = useState('');

  // Agents par agence
  const [modalAgents, setModalAgents]               = useState(false);
  const [agentsAgence, setAgentsAgence]             = useState([]);
  const [agentsLoading, setAgentsLoading]           = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const [agRes, dirRes, agentRes, assignRes] = await Promise.allSettled([
        getAllAgencies(),
        authApiClient.get('/auth/users/by-role/DIRECTEUR_AGENCE'),
        authApiClient.get('/auth/users/by-role/AGENT'),
        getAllActiveAssignments(),
      ]);
      setAgences(agRes.status === 'fulfilled' ? (agRes.value.data ?? []) : []);
      setDirecteurs(dirRes.status === 'fulfilled' ? (dirRes.value.data ?? []) : []);
      setAgents(agentRes.status === 'fulfilled' ? (agentRes.value.data ?? []) : []);
      if (assignRes.status === 'fulfilled') {
        const map = {};
        (assignRes.value.data ?? []).forEach(a => { map[a.agentId] = a; });
        setAgentAssignments(map);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  // ── Créer une agence ────────────────────────────────────────────────────────
  const handleCreer = async (e) => {
    e.preventDefault();
    setErreur('');
    setSaving(true);
    try {
      await createAgency(form);
      await loadData();
      setModalCreation(false);
      setForm(EMPTY_AGENCY_FORM);
    } catch (err) {
      setErreur(err.response?.data?.message ?? 'Erreur lors de la création');
    } finally {
      setSaving(false);
    }
  };

  // ── Assigner un directeur existant ─────────────────────────────────────────
  const handleAssignerDirecteur = async (e) => {
    e.preventDefault();
    if (!agenceSelectee || !directeurId) return;
    setErreur('');
    setSaving(true);
    try {
      await assignDirecteur(agenceSelectee.id, directeurId);
      await loadData();
      setModalDirecteur(false);
      setAgenceSelectee(null);
      setDirecteurId('');
    } catch (err) {
      setErreur(err.response?.data?.message ?? "Erreur lors de l'assignation");
    } finally {
      setSaving(false);
    }
  };

  // ── Créer un directeur d'agence ────────────────────────────────────────────
  const handleCreerDirecteur = async (e) => {
    e.preventDefault();
    setErreur('');
    setSaving(true);
    const result = await registerDirecteur(newDir);
    if (!result.success) {
      setErreur(result.error);
      setSaving(false);
      return;
    }
    await loadData();
    setModalNouveauDir(false);
    setNewDir(EMPTY_DIR_FORM);
    setSaving(false);
  };

  // ── Assigner un agent à une agence ─────────────────────────────────────────
  const doAssignerAgent = async (aid) => {
    setErreur('');
    setSaving(true);
    try {
      await assignAgent(agenceSelectee.id, aid);
      await loadData();
      setModalAgent(false);
      setConfirmAgentId(null);
      setAgenceSelectee(null);
      setAgentId('');
    } catch (err) {
      setErreur(err.response?.data?.message ?? "Erreur lors de l'assignation de l'agent");
    } finally {
      setSaving(false);
    }
  };

  const handleAssignerAgent = async (e) => {
    e.preventDefault();
    if (!agenceSelectee || !agentId) return;
    const existingAssignment = agentAssignments[agentId];
    if (existingAssignment && existingAssignment.agencyId !== agenceSelectee.id) {
      // Agent déjà dans une autre agence → demander confirmation
      setConfirmAgentId(agentId);
    } else {
      await doAssignerAgent(agentId);
    }
  };

  // ── Toggle statut ──────────────────────────────────────────────────────────
  const handleToggle = async (agence) => {
    setToggleErreur('');
    try {
      await toggleAgencyStatus(agence.id);
      await loadData();
    } catch (err) {
      setToggleErreur(err.response?.data?.message ?? "Erreur lors du changement de statut de l'agence");
    }
  };

  const handleRetirerAgent = async (agentId) => {
    setRemovingAgent(agentId);
    try {
      await unassignAgentAdmin(agentId);
      setAgentsAgence(prev => prev.filter(a => a.agentId !== agentId));
      await loadData();
    } catch (err) {
      setToggleErreur(err.response?.data?.message ?? "Erreur lors du retrait de l'agent");
    } finally {
      setRemovingAgent(null);
    }
  };

  const handleRetirerDirecteur = async () => {
    if (!agenceSelectee) return;
    setRemovingDirector(true);
    try {
      await unassignDirectorAdmin(agenceSelectee.id);
      await loadData();
      setModalDirecteur(false);
      setAgenceSelectee(null);
    } catch (err) {
      setErreur(err.response?.data?.message ?? "Erreur lors du retrait du directeur");
    } finally {
      setRemovingDirector(false);
    }
  };

  const openModalDirecteur = (agence) => {
    setAgenceSelectee(agence);
    setDirecteurId(agence.directorId ?? '');
    setErreur('');
    setModalDirecteur(true);
  };

  const openModalAgent = (agence) => {
    setAgenceSelectee(agence);
    setAgentId('');
    setErreur('');
    setModalAgent(true);
  };

  const openAgents = async (agence) => {
    setAgenceSelectee(agence);
    setAgentsAgence([]);
    setModalAgents(true);
    setAgentsLoading(true);
    try {
      const res = await getAgencyAgents(agence.id);
      setAgentsAgence(res.data ?? []);
    } catch {
      setAgentsAgence([]);
    } finally {
      setAgentsLoading(false);
    }
  };

  const ouvertes    = agences.filter(a => a.status === 'ACTIVE').length;
  const fermees     = agences.filter(a => a.status !== 'ACTIVE').length;
  const totalAgents = agences.reduce((sum, a) => sum + (a.agentsCount ?? 0), 0);

  return (
    <div className="p-6 space-y-6">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Gestion des Agences</h1>
          <p className="text-gray-500 text-sm">Créez et gérez les agences du réseau MicroFinanceHub</p>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={() => { setModalNouveauDir(true); setErreur(''); }} className="btn-secondary flex items-center gap-2">
            <UserPlus size={16} /> Créer un Directeur
          </button>
          <button onClick={() => setModalCreation(true)} className="btn-primary flex items-center gap-2">
            <Plus size={16} /> Nouvelle Agence
          </button>
        </div>
      </div>

      {toggleErreur && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
          {toggleErreur}
        </div>
      )}

      <div className="grid grid-cols-4 gap-4">
        <StatCard title="Total Agences"   value={String(agences.length)} icon={Building2}  iconBg="bg-blue-100"   iconColor="text-blue-600" />
        <StatCard title="Agences Actives" value={String(ouvertes)}       icon={ToggleRight} iconBg="bg-green-100" iconColor="text-green-600" />
        <StatCard title="Agences Fermées" value={String(fermees)}        icon={ToggleLeft}  iconBg="bg-red-100"   iconColor="text-red-500" />
        <StatCard title="Agents au Total" value={String(totalAgents)}    icon={Users}       iconBg="bg-orange-100" iconColor="text-orange-600" />
      </div>

      <div className="card">
        <h3 className="font-semibold text-gray-700 mb-1">Liste des Agences</h3>
        <p className="text-xs text-gray-400 mb-4">
          {loading ? 'Chargement...' : agences.length === 0
            ? 'Aucune agence — utilisez "Nouvelle Agence" pour en créer une'
            : `${agences.length} agence(s)`}
        </p>
        <table className="w-full text-sm">
          <thead>
            <tr className="text-xs text-gray-400 border-b border-gray-100">
              <th className="text-left pb-3">Code</th>
              <th className="text-left pb-3">Nom</th>
              <th className="text-left pb-3">Ville / Région</th>
              <th className="text-left pb-3">Directeur</th>
              <th className="text-left pb-3">Agents</th>
              <th className="text-left pb-3">Statut</th>
              <th className="text-left pb-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="py-8 text-center text-gray-400">Chargement des agences...</td></tr>
            ) : agences.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-12 text-center text-gray-300">
                  <Building2 size={32} className="mx-auto mb-2" />
                  <p className="text-sm">Aucune agence — cliquez sur "Nouvelle Agence" pour en créer une</p>
                </td>
              </tr>
            ) : (
              agences.map(a => (
                <tr key={a.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="py-3 font-mono text-xs font-semibold text-blue-700">{a.code}</td>
                  <td className="py-3 font-medium text-gray-800">{a.name}</td>
                  <td className="py-3 text-gray-500 text-xs">
                    <div className="flex items-center gap-1">
                      <MapPin size={12} />
                      <span>{a.city ?? '—'}{a.region ? ` · ${a.region}` : ''}</span>
                    </div>
                  </td>
                  <td className="py-3 text-xs">
                    {a.directorName
                      ? <span className="text-gray-700">{a.directorName}</span>
                      : <span className="text-gray-400 italic">Non assigné</span>}
                  </td>
                  <td className="py-3 text-center">
                    <span className="inline-flex items-center gap-1 text-xs text-gray-600">
                      <UserCheck size={13} />{a.agentsCount ?? 0}
                    </span>
                  </td>
                  <td className="py-3">
                    <Badge status={a.status === 'ACTIVE' ? 'actif' : 'inactif'} />
                  </td>
                  <td className="py-3">
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => openAgents(a)}
                        className="p-1.5 hover:bg-indigo-50 text-gray-400 hover:text-indigo-600 rounded-lg"
                        title="Voir les agents"
                      >
                        <Users size={15} />
                      </button>
                      <button
                        onClick={() => openModalDirecteur(a)}
                        className="p-1.5 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded-lg"
                        title="Assigner un directeur"
                      >
                        <UserCog size={15} />
                      </button>
                      <button
                        onClick={() => openModalAgent(a)}
                        className="p-1.5 hover:bg-green-50 text-gray-400 hover:text-green-600 rounded-lg"
                        title="Assigner un agent"
                      >
                        <UserPlus size={15} />
                      </button>
                      <button
                        onClick={() => handleToggle(a)}
                        className="p-1.5 hover:bg-orange-50 text-gray-400 hover:text-orange-500 rounded-lg"
                        title={a.status === 'ACTIVE' ? 'Désactiver' : 'Activer'}
                      >
                        {a.status === 'ACTIVE'
                          ? <ToggleRight size={15} className="text-green-500" />
                          : <ToggleLeft size={15} className="text-red-400" />}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* ── Modal Création Agence ───────────────────────────────────────────── */}
      <Modal isOpen={modalCreation} onClose={() => { setModalCreation(false); setErreur(''); }}
        title="Créer une Agence" subtitle="Renseignez les informations de la nouvelle agence">
        <form onSubmit={handleCreer} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Code (5 chiffres) *</label>
              <input value={form.code} onChange={e => setForm(p => ({ ...p, code: e.target.value }))}
                placeholder="Ex: 00001" pattern="[0-9]{5}" maxLength={5} required className="form-input" />
            </div>
            <div>
              <label className="form-label">Nom *</label>
              <input value={form.name} onChange={e => setForm(p => ({ ...p, name: e.target.value }))}
                placeholder="Ex: Agence Centrale Douala" required className="form-input" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Ville</label>
              <input value={form.city} onChange={e => setForm(p => ({ ...p, city: e.target.value }))}
                placeholder="Ex: Douala" className="form-input" />
            </div>
            <div>
              <label className="form-label">Région</label>
              <input value={form.region} onChange={e => setForm(p => ({ ...p, region: e.target.value }))}
                placeholder="Ex: Littoral" className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Adresse</label>
            <input value={form.address} onChange={e => setForm(p => ({ ...p, address: e.target.value }))}
              placeholder="Ex: Rue Joss, Akwa" className="form-input" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Téléphone</label>
              <input value={form.phoneNumber} onChange={e => setForm(p => ({ ...p, phoneNumber: e.target.value }))}
                placeholder="+237 2XX XXX XXX" className="form-input" />
            </div>
            <div>
              <label className="form-label">Email Agence</label>
              <input type="email" value={form.email} onChange={e => setForm(p => ({ ...p, email: e.target.value }))}
                placeholder="agence@mfh.cm" className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Directeur <span className="text-gray-400 font-normal">(optionnel)</span></label>
            <select value={form.directorId} onChange={e => setForm(p => ({ ...p, directorId: e.target.value }))}
              className="form-input">
              <option value="">— Aucun directeur pour l'instant —</option>
              {directeurs
                .filter(d => !agences.some(a => a.directorId === d.id))
                .map(d => (
                  <option key={d.id} value={d.id}>{d.firstName} {d.lastName} ({d.email})</option>
                ))}
            </select>
            {directeurs.filter(d => !agences.some(a => a.directorId === d.id)).length === 0 && (
              <p className="text-xs text-amber-600 mt-1">
                Tous les directeurs sont déjà assignés. Vous pourrez en assigner un plus tard.
              </p>
            )}
          </div>
          {erreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{erreur}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalCreation(false); setErreur(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={saving} className="btn-primary disabled:opacity-60">
              {saving ? 'Création...' : "Créer l'Agence"}
            </button>
          </div>
        </form>
      </Modal>

      {/* ── Modal Créer un Directeur ────────────────────────────────────────── */}
      <Modal isOpen={modalNouveauDir} onClose={() => { setModalNouveauDir(false); setErreur(''); }}
        title="Créer un Directeur d'Agence" subtitle="Un compte sera créé avec le rôle DIRECTEUR_AGENCE">
        <form onSubmit={handleCreerDirecteur} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Nom *</label>
              <input value={newDir.nom} onChange={e => setNewDir(p => ({ ...p, nom: e.target.value }))}
                placeholder="Ex: Mbida" required className="form-input" />
            </div>
            <div>
              <label className="form-label">Prénom *</label>
              <input value={newDir.prenom} onChange={e => setNewDir(p => ({ ...p, prenom: e.target.value }))}
                placeholder="Ex: Axelle" required className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Email *</label>
            <input type="email" value={newDir.email} onChange={e => setNewDir(p => ({ ...p, email: e.target.value }))}
              placeholder="directeur@mfh.cm" required className="form-input" />
          </div>
          <div>
            <label className="form-label">Téléphone</label>
            <input value={newDir.telephone} onChange={e => setNewDir(p => ({ ...p, telephone: e.target.value }))}
              placeholder="+237 6XX XXX XXX" className="form-input" />
          </div>
          <div>
            <label className="form-label">Mot de Passe Provisoire *</label>
            <input type="password" value={newDir.motDePasse} onChange={e => setNewDir(p => ({ ...p, motDePasse: e.target.value }))}
              placeholder="Minimum 6 caractères" required minLength={6} className="form-input" />
          </div>
          {erreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{erreur}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalNouveauDir(false); setErreur(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={saving} className="btn-primary disabled:opacity-60">
              {saving ? 'Création...' : 'Créer le Directeur'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ── Modal Assigner Directeur ────────────────────────────────────────── */}
      <Modal isOpen={modalDirecteur} onClose={() => { setModalDirecteur(false); setErreur(''); }}
        title="Assigner un Directeur"
        subtitle={agenceSelectee ? `Agence ${agenceSelectee.code} — ${agenceSelectee.name}` : ''}>
        <form onSubmit={handleAssignerDirecteur} className="space-y-4">
          {agenceSelectee?.directorName && (
            <div className="bg-blue-50 text-blue-700 text-sm px-3 py-2 rounded-lg flex items-center justify-between">
              <span>Directeur actuel : <strong>{agenceSelectee.directorName}</strong></span>
              <button
                type="button"
                onClick={handleRetirerDirecteur}
                disabled={removingDirector}
                className="flex items-center gap-1 text-xs text-red-500 hover:text-red-700 bg-white hover:bg-red-50 border border-red-200 px-2 py-1 rounded disabled:opacity-50 ml-3 shrink-0"
              >
                <UserX size={13} />
                {removingDirector ? '...' : 'Retirer'}
              </button>
            </div>
          )}
          <div>
            <label className="form-label">Nouveau Directeur *</label>
            <select value={directeurId} onChange={e => setDirecteurId(e.target.value)} required className="form-input">
              <option value="">— Choisir un directeur —</option>
              {directeurs
                .filter(d => !agences.some(a => a.directorId === d.id && a.id !== agenceSelectee?.id))
                .map(d => (
                  <option key={d.id} value={d.id}>{d.firstName} {d.lastName} ({d.email})</option>
                ))}
            </select>
            {directeurs.filter(d => !agences.some(a => a.directorId === d.id && a.id !== agenceSelectee?.id)).length === 0 && (
              <p className="text-xs text-amber-600 mt-1">Tous les directeurs sont déjà assignés à d'autres agences.</p>
            )}
          </div>
          {erreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{erreur}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalDirecteur(false); setErreur(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={saving || !directeurId} className="btn-primary disabled:opacity-60">
              {saving ? 'Assignation...' : 'Assigner'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ── Modal Agents de l'agence ────────────────────────────────────────── */}
      <Modal
        isOpen={modalAgents}
        onClose={() => { setModalAgents(false); setAgentsAgence([]); }}
        title="Agents de l'agence"
        subtitle={agenceSelectee ? `${agenceSelectee.code} — ${agenceSelectee.name}` : ''}>
        <div className="space-y-3 min-w-[420px]">
          {agentsLoading ? (
            <p className="text-center text-gray-400 py-6 text-sm">Chargement...</p>
          ) : agentsAgence.length === 0 ? (
            <div className="text-center py-8 text-gray-300">
              <UserCheck size={28} className="mx-auto mb-2" />
              <p className="text-sm">Aucun agent assigné à cette agence</p>
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-xs text-gray-400 border-b border-gray-100">
                  <th className="text-left pb-2">Nom</th>
                  <th className="text-left pb-2">Email</th>
                  <th className="text-left pb-2">Assigné le</th>
                  <th className="pb-2"></th>
                </tr>
              </thead>
              <tbody>
                {agentsAgence.map(a => (
                  <tr key={a.agentId ?? a.id} className="border-b border-gray-50">
                    <td className="py-2 font-medium text-gray-800">{a.agentName}</td>
                    <td className="py-2 text-gray-500 text-xs">{a.agentEmail}</td>
                    <td className="py-2 text-xs text-gray-400">
                      {a.assignedAt ? new Date(a.assignedAt).toLocaleDateString('fr-FR') : '—'}
                    </td>
                    <td className="py-2 text-right">
                      <button
                        onClick={() => handleRetirerAgent(a.agentId ?? a.id)}
                        disabled={removingAgent === (a.agentId ?? a.id)}
                        className="flex items-center gap-1 text-xs text-red-500 hover:text-red-700 hover:bg-red-50 px-2 py-1 rounded disabled:opacity-50"
                        title="Retirer de l'agence"
                      >
                        <UserX size={13} />
                        {removingAgent === (a.agentId ?? a.id) ? '...' : 'Retirer'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <div className="flex justify-end pt-2">
            <button onClick={() => { setModalAgents(false); setAgentsAgence([]); }} className="btn-secondary">
              Fermer
            </button>
          </div>
        </div>
      </Modal>

      {/* ── Modal Assigner un Agent ─────────────────────────────────────────── */}
      <Modal isOpen={modalAgent} onClose={() => { setModalAgent(false); setErreur(''); setConfirmAgentId(null); }}
        title="Assigner un Agent"
        subtitle={agenceSelectee ? `Agence ${agenceSelectee.code} — ${agenceSelectee.name}` : ''}>

        {confirmAgentId ? (
          <div className="space-y-4">
            <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-sm text-amber-800">
              <p className="font-semibold mb-1">Attention</p>
              <p>
                Cet agent appartient déjà à l'agence{' '}
                <strong>{agentAssignments[confirmAgentId]?.agencyName} ({agentAssignments[confirmAgentId]?.agencyCode})</strong>.
                Êtes-vous sûr de vouloir l'assigner à l'agence <strong>{agenceSelectee?.name}</strong> ?
              </p>
            </div>
            {erreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{erreur}</p>}
            <div className="flex justify-end gap-3">
              <button onClick={() => setConfirmAgentId(null)} className="btn-secondary">Non, annuler</button>
              <button onClick={() => doAssignerAgent(confirmAgentId)} disabled={saving}
                className="btn-primary disabled:opacity-60">
                {saving ? 'Assignation...' : 'Oui, changer l\'agence'}
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleAssignerAgent} className="space-y-4">
            <div>
              <label className="form-label">Agent *</label>
              <select value={agentId} onChange={e => setAgentId(e.target.value)} required className="form-input">
                <option value="">— Choisir un agent —</option>
                {agents
                  .filter(a => agentAssignments[a.id]?.agencyId !== agenceSelectee?.id)
                  .map(a => {
                    const assignment = agentAssignments[a.id];
                    return (
                      <option key={a.id} value={a.id}>
                        {a.firstName} {a.lastName} ({a.email})
                        {assignment ? ` — actuellement ${assignment.agencyCode}` : ''}
                      </option>
                    );
                  })}
              </select>
              {agents.filter(a => agentAssignments[a.id]?.agencyId !== agenceSelectee?.id).length === 0 && (
                <p className="text-xs text-amber-600 mt-1">
                  Tous les agents disponibles sont déjà dans cette agence.
                </p>
              )}
            </div>
            {erreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{erreur}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => { setModalAgent(false); setErreur(''); }} className="btn-secondary">Annuler</button>
              <button type="submit" disabled={saving || !agentId} className="btn-primary disabled:opacity-60">
                {saving ? 'Assignation...' : "Assigner l'Agent"}
              </button>
            </div>
          </form>
        )}
      </Modal>

    </div>
  );
};

export default AgencesPage;
