import React, { useState, useEffect } from 'react';
import { UserCog, Users, Building2, Plus, Pencil, ToggleRight, ToggleLeft, Trash2 } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import { registerDirecteur, updateUser, deleteUser, toggleUser } from '../../api/authApi';
import { getAllAgencies, assignDirecteur } from '../../api/agencyApi';
import authApiClient from '../../api/authApiClient';

const EMPTY_FORM = { nom: '', prenom: '', email: '', telephone: '', motDePasse: '' };

const DirecteursPage = () => {
  const [directeurs, setDirecteurs]         = useState([]);
  const [agences, setAgences]               = useState([]);
  const [loading, setLoading]               = useState(true);
  const [modalCreation, setModalCreation]   = useState(false);
  const [modalAgence, setModalAgence]       = useState(false);
  const [directeurSelecte, setDirecteurSelecte] = useState(null);
  const [agenceId, setAgenceId]             = useState('');
  const [form, setForm]                     = useState(EMPTY_FORM);
  const [saving, setSaving]                 = useState(false);
  const [erreur, setErreur]                 = useState('');

  // Modal édition
  const [modalEdit, setModalEdit]           = useState(false);
  const [dirEdit, setDirEdit]               = useState(null);
  const [editForm, setEditForm]             = useState({});
  const [editSaving, setEditSaving]         = useState(false);
  const [editErreur, setEditErreur]         = useState('');

  // Modal suppression
  const [modalSupp, setModalSupp]           = useState(false);
  const [dirSupp, setDirSupp]               = useState(null);
  const [suppLoading, setSuppLoading]       = useState(false);
  const [suppErreur, setSuppErreur]         = useState('');

  const loadData = async () => {
    setLoading(true);
    try {
      const [dirRes, agRes] = await Promise.allSettled([
        authApiClient.get('/auth/users/by-role/DIRECTEUR_AGENCE'),
        getAllAgencies(),
      ]);
      setDirecteurs(dirRes.status === 'fulfilled' ? (dirRes.value.data ?? []) : []);
      setAgences(agRes.status === 'fulfilled' ? (agRes.value.data ?? []) : []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  // Retrouve l'agence gérée par un directeur donné
  const agenceDuDirecteur = (dirId) =>
    agences.find(a => a.directorId === dirId);

  const handleCreer = async (e) => {
    e.preventDefault();
    setErreur('');
    setSaving(true);
    const result = await registerDirecteur(form);
    if (!result.success) {
      setErreur(result.error);
      setSaving(false);
      return;
    }
    await loadData();
    setModalCreation(false);
    setForm(EMPTY_FORM);
    setSaving(false);
  };

  const openModalAgence = (dir) => {
    setDirecteurSelecte(dir);
    // Pré-sélectionner l'agence actuelle si elle existe
    const agence = agenceDuDirecteur(dir.id);
    setAgenceId(agence?.id ?? '');
    setErreur('');
    setModalAgence(true);
  };

  const handleAssignerAgence = async (e) => {
    e.preventDefault();
    if (!directeurSelecte || !agenceId) return;
    setErreur('');
    setSaving(true);
    try {
      await assignDirecteur(agenceId, directeurSelecte.id);
      await loadData();
      setModalAgence(false);
      setDirecteurSelecte(null);
      setAgenceId('');
    } catch (err) {
      setErreur(err.response?.data?.message ?? "Erreur lors de l'assignation");
    } finally {
      setSaving(false);
    }
  };

  const openEdit = (dir) => {
    setDirEdit(dir);
    setEditForm({ firstName: dir.firstName ?? '', lastName: dir.lastName ?? '', phoneNumber: dir.phoneNumber ?? '' });
    setEditErreur('');
    setModalEdit(true);
  };

  const handleEdit = async (e) => {
    e.preventDefault();
    if (!dirEdit) return;
    setEditSaving(true);
    setEditErreur('');
    try {
      await updateUser(dirEdit.id, editForm);
      await loadData();
      setModalEdit(false);
      setDirEdit(null);
    } catch (err) {
      setEditErreur(err.response?.data?.message ?? 'Erreur lors de la modification');
    } finally {
      setEditSaving(false);
    }
  };

  const handleToggle = async (dir) => {
    try {
      await toggleUser(dir.id);
      await loadData();
    } catch {
      // silencieux
    }
  };

  const openSupp = (dir) => {
    setDirSupp(dir);
    setSuppErreur('');
    setModalSupp(true);
  };

  const handleSupprimer = async () => {
    if (!dirSupp) return;
    setSuppLoading(true);
    setSuppErreur('');
    try {
      await deleteUser(dirSupp.id);
      await loadData();
      setModalSupp(false);
      setDirSupp(null);
    } catch (err) {
      setSuppErreur(err.response?.data?.message ?? 'Erreur lors de la suppression');
    } finally {
      setSuppLoading(false);
    }
  };

  const actifs   = directeurs.filter(d => d.enabled).length;
  const assigns  = directeurs.filter(d => agenceDuDirecteur(d.id)).length;

  return (
    <div className="p-6 space-y-6">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Directeurs d'Agence</h1>
          <p className="text-gray-500 text-sm">Créez et gérez les directeurs d'agences du réseau</p>
        </div>
        <button onClick={() => { setModalCreation(true); setErreur(''); }} className="btn-primary flex items-center gap-2">
          <Plus size={16} /> Nouveau Directeur
        </button>
      </div>

      <div className="grid grid-cols-3 gap-4">
        <StatCard title="Total Directeurs"  value={String(directeurs.length)} icon={UserCog} iconBg="bg-blue-100"   iconColor="text-blue-600" />
        <StatCard title="Actifs"            value={String(actifs)}            icon={Users}   iconBg="bg-green-100"  iconColor="text-green-600" />
        <StatCard title="Assignés à Agence" value={String(assigns)}           icon={Building2} iconBg="bg-purple-100" iconColor="text-purple-600" />
      </div>

      <div className="card">
        <h3 className="font-semibold text-gray-700 mb-1">Liste des Directeurs</h3>
        <p className="text-xs text-gray-400 mb-4">
          {loading ? 'Chargement...' : directeurs.length === 0
            ? 'Aucun directeur — utilisez "Nouveau Directeur" pour en créer un'
            : `${directeurs.length} directeur(s)`}
        </p>
        <table className="w-full text-sm">
          <thead>
            <tr className="text-xs text-gray-400 border-b border-gray-100">
              <th className="text-left pb-3">Nom</th>
              <th className="text-left pb-3">Email</th>
              <th className="text-left pb-3">Téléphone</th>
              <th className="text-left pb-3">Statut</th>
              <th className="text-left pb-3">Agence Assignée</th>
              <th className="text-left pb-3">Créé le</th>
              <th className="text-left pb-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="py-8 text-center text-gray-400">Chargement...</td></tr>
            ) : directeurs.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-12 text-center text-gray-300">
                  <UserCog size={32} className="mx-auto mb-2" />
                  <p className="text-sm">Aucun directeur — cliquez sur "Nouveau Directeur" pour créer le premier</p>
                </td>
              </tr>
            ) : (
              directeurs.map(d => {
                const agence = agenceDuDirecteur(d.id);
                return (
                  <tr key={d.id} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 font-medium text-gray-800">{d.firstName} {d.lastName}</td>
                    <td className="py-3 text-gray-500 text-xs">{d.email}</td>
                    <td className="py-3 text-gray-600">{d.phoneNumber || '—'}</td>
                    <td className="py-3"><Badge status={d.enabled ? 'actif' : 'inactif'} /></td>
                    <td className="py-3 text-xs">
                      {agence
                        ? <span className="inline-flex items-center gap-1 text-blue-700 font-medium">
                            <Building2 size={12} />{agence.code} — {agence.name}
                          </span>
                        : <span className="text-gray-400 italic">Non assigné</span>}
                    </td>
                    <td className="py-3 text-xs text-gray-400">
                      {d.createdAt ? new Date(d.createdAt).toLocaleDateString('fr-FR') : '—'}
                    </td>
                    <td className="py-3">
                      <div className="flex items-center gap-1">
                        <button onClick={() => openEdit(d)} title="Modifier"
                          className="p-1.5 hover:bg-yellow-50 text-gray-400 hover:text-yellow-600 rounded-lg">
                          <Pencil size={15} />
                        </button>
                        <button onClick={() => handleToggle(d)} title={d.enabled ? 'Désactiver' : 'Activer'}
                          className="p-1.5 hover:bg-indigo-50 text-gray-400 hover:text-indigo-600 rounded-lg">
                          {d.enabled ? <ToggleRight size={15} /> : <ToggleLeft size={15} />}
                        </button>
                        <button onClick={() => openModalAgence(d)} title="Assigner à une agence"
                          className="p-1.5 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded-lg">
                          <Building2 size={15} />
                        </button>
                        <button onClick={() => openSupp(d)} title="Supprimer"
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

      {/* ── Modal Créer un Directeur ──────────────────────────────────────── */}
      <Modal isOpen={modalCreation} onClose={() => { setModalCreation(false); setErreur(''); }}
        title="Créer un Directeur d'Agence" subtitle="Un compte avec le rôle DIRECTEUR_AGENCE sera créé">
        <form onSubmit={handleCreer} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Nom *</label>
              <input value={form.nom} onChange={e => setForm(p => ({ ...p, nom: e.target.value }))}
                placeholder="Ex: Mbida" required className="form-input" />
            </div>
            <div>
              <label className="form-label">Prénom *</label>
              <input value={form.prenom} onChange={e => setForm(p => ({ ...p, prenom: e.target.value }))}
                placeholder="Ex: Axelle" required className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Email *</label>
            <input type="email" value={form.email} onChange={e => setForm(p => ({ ...p, email: e.target.value }))}
              placeholder="directeur@mfh.cm" required className="form-input" />
          </div>
          <div>
            <label className="form-label">Téléphone</label>
            <input value={form.telephone} onChange={e => setForm(p => ({ ...p, telephone: e.target.value }))}
              placeholder="+237 6XX XXX XXX" className="form-input" />
          </div>
          <div>
            <label className="form-label">Mot de Passe Provisoire *</label>
            <input type="password" value={form.motDePasse} onChange={e => setForm(p => ({ ...p, motDePasse: e.target.value }))}
              placeholder="Minimum 6 caractères" required minLength={6} className="form-input" />
          </div>
          {erreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{erreur}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalCreation(false); setErreur(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={saving} className="btn-primary disabled:opacity-60">
              {saving ? 'Création...' : 'Créer le Directeur'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ── Modal Édition ────────────────────────────────────────────────── */}
      <Modal isOpen={modalEdit} onClose={() => { setModalEdit(false); setEditErreur(''); }}
        title="Modifier le Directeur"
        subtitle={dirEdit ? `${dirEdit.firstName} ${dirEdit.lastName}` : ''}>
        <form onSubmit={handleEdit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Prénom *</label>
              <input value={editForm.firstName ?? ''} onChange={e => setEditForm(p => ({ ...p, firstName: e.target.value }))}
                required className="form-input" />
            </div>
            <div>
              <label className="form-label">Nom *</label>
              <input value={editForm.lastName ?? ''} onChange={e => setEditForm(p => ({ ...p, lastName: e.target.value }))}
                required className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Téléphone</label>
            <input value={editForm.phoneNumber ?? ''} onChange={e => setEditForm(p => ({ ...p, phoneNumber: e.target.value }))}
              className="form-input" />
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

      {/* ── Modal Suppression ─────────────────────────────────────────────── */}
      <Modal isOpen={modalSupp} onClose={() => { setModalSupp(false); setSuppErreur(''); }}
        title="Supprimer le Directeur"
        subtitle={dirSupp ? `${dirSupp.firstName} ${dirSupp.lastName}` : ''}>
        <div className="space-y-4">
          <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-700">
            <p className="font-semibold mb-1">Cette action est irréversible.</p>
            <p>Le compte de ce directeur sera définitivement supprimé.</p>
          </div>
          <p className="text-sm text-gray-600">
            Confirmez-vous la suppression de <strong>{dirSupp?.firstName} {dirSupp?.lastName}</strong> ?
          </p>
          {suppErreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{suppErreur}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalSupp(false); setSuppErreur(''); }} className="btn-secondary">Annuler</button>
            <button onClick={handleSupprimer} disabled={suppLoading}
              className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg text-sm font-medium disabled:opacity-60 flex items-center gap-2">
              <Trash2 size={14} /> {suppLoading ? 'Suppression...' : 'Confirmer la suppression'}
            </button>
          </div>
        </div>
      </Modal>

      {/* ── Modal Assigner à une Agence ──────────────────────────────────── */}
      <Modal isOpen={modalAgence} onClose={() => { setModalAgence(false); setErreur(''); }}
        title="Assigner à une Agence"
        subtitle={directeurSelecte ? `${directeurSelecte.firstName} ${directeurSelecte.lastName}` : ''}>
        <form onSubmit={handleAssignerAgence} className="space-y-4">
          {directeurSelecte && agenceDuDirecteur(directeurSelecte.id) && (
            <div className="bg-blue-50 text-blue-700 text-sm px-3 py-2 rounded-lg">
              Agence actuelle : <strong>{agenceDuDirecteur(directeurSelecte.id).name}</strong>
            </div>
          )}
          <div>
            <label className="form-label">Agence *</label>
            <select value={agenceId} onChange={e => setAgenceId(e.target.value)} required className="form-input">
              <option value="">— Choisir une agence —</option>
              {agences
                .filter(a =>
                  // Agences sans directeur OU l'agence déjà assignée à ce directeur
                  !a.directorId || a.directorId === directeurSelecte?.id
                )
                .map(a => (
                  <option key={a.id} value={a.id}>{a.code} — {a.name} ({a.city ?? '—'})</option>
                ))}
            </select>
            {agences.filter(a => !a.directorId || a.directorId === directeurSelecte?.id).length === 0 && (
              <p className="text-xs text-amber-600 mt-1">
                Toutes les agences ont déjà un directeur. Créez une nouvelle agence d'abord.
              </p>
            )}
          </div>
          {erreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{erreur}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalAgence(false); setErreur(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={saving || !agenceId} className="btn-primary disabled:opacity-60">
              {saving ? 'Assignation...' : 'Assigner'}
            </button>
          </div>
        </form>
      </Modal>

    </div>
  );
};

export default DirecteursPage;
