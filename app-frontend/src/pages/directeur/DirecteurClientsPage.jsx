import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, Search, UserCheck, UserPlus, UserX, Eye, Edit2, ToggleRight, ToggleLeft } from 'lucide-react';
import Badge from '../../components/common/Badge';
import StatCard from '../../components/common/StatCard';
import Modal from '../../components/common/Modal';
import { getMyAgency, getMyAgencyClients } from '../../api/agencyApi';
import { createClient, updateClient, updateClientStatus } from '../../api/clientsApi';

const DirecteurClientsPage = () => {
  const navigate = useNavigate();
  const [clients, setClients]   = useState([]);
  const [agency, setAgency]     = useState(null);
  const [search, setSearch]     = useState('');
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState('');

  const [modalCreate, setModalCreate] = useState(false);
  const [modalEdit,   setModalEdit]   = useState(null);
  const [saving,      setSaving]      = useState(false);
  const [formError,   setFormError]   = useState('');

  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', phoneNumber: '',
    address: '', clientType: 'INDIVIDUAL',
  });
  const [editForm, setEditForm] = useState({ firstName: '', lastName: '', phoneNumber: '', address: '' });

  const loadData = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [agencyRes, clientsRes] = await Promise.all([getMyAgency(), getMyAgencyClients()]);
      setAgency(agencyRes.data);
      setClients((clientsRes.data?.clients ?? []).map(c => ({
        id: c.clientId, firstName: c.clientFirstName, lastName: c.clientLastName,
        email: c.clientEmail, phoneNumber: c.clientPhone, status: c.clientStatus,
        creditScore: c.clientCreditScore, createdBy: c.clientCreatedBy, createdAt: c.clientCreatedAt,
      })));
    } catch { setError("Impossible de charger les clients de l'agence."); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const filtered = clients.filter(c =>
    `${c.firstName} ${c.lastName} ${c.email}`.toLowerCase().includes(search.toLowerCase())
  );
  const active    = clients.filter(c => c.status?.toString() === 'ACTIVE');
  const pending   = clients.filter(c => c.status?.toString() === 'PENDING');
  const thisMonth = clients.filter(c => {
    if (!c.createdAt) return false;
    const d = new Date(c.createdAt); const now = new Date();
    return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
  });

  const openCreate = () => {
    setForm({ firstName: '', lastName: '', email: '', phoneNumber: '', address: '', clientType: 'INDIVIDUAL' });
    setFormError(''); setModalCreate(true);
  };
  const handleCreate = async (e) => {
    e.preventDefault(); setSaving(true); setFormError('');
    try {
      await createClient({ ...form, email: form.email.trim().toLowerCase(), agencyId: agency?.id ?? null });
      setModalCreate(false); await loadData();
    } catch (err) { setFormError(err.response?.data?.message ?? 'Erreur lors de la création.'); }
    finally { setSaving(false); }
  };

  const openEdit = (client) => {
    setEditForm({ firstName: client.firstName ?? '', lastName: client.lastName ?? '', phoneNumber: client.phoneNumber ?? '', address: '' });
    setFormError(''); setModalEdit(client);
  };
  const handleEdit = async (e) => {
    e.preventDefault(); setSaving(true); setFormError('');
    try {
      await updateClient(modalEdit.id, editForm);
      setModalEdit(null); await loadData();
    } catch (err) { setFormError(err.response?.data?.message ?? 'Erreur lors de la mise à jour.'); }
    finally { setSaving(false); }
  };

  const handleToggleStatus = async (client) => {
    const newStatus = client.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try { await updateClientStatus(client.id, newStatus); await loadData(); }
    catch (err) { alert(err.response?.data?.message ?? 'Erreur changement statut.'); }
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Clients de l'Agence</h1>
          <p className="text-gray-500 text-sm">{agency ? `${agency.code} — ${agency.name}` : 'Chargement…'}</p>
        </div>
        <button onClick={openCreate} className="btn-primary flex items-center gap-2">
          <UserPlus size={15} /> Nouveau Client
        </button>
      </div>

      <div className="grid grid-cols-4 gap-4">
        <StatCard title="Total"      value={loading ? '…' : String(clients.length)}   icon={Users}     iconBg="bg-blue-100"   iconColor="text-blue-600" />
        <StatCard title="Actifs"     value={loading ? '…' : String(active.length)}    icon={UserCheck} iconBg="bg-green-100"  iconColor="text-green-600" />
        <StatCard title="En attente" value={loading ? '…' : String(pending.length)}   icon={UserPlus}  iconBg="bg-orange-100" iconColor="text-orange-500" />
        <StatCard title="Ce mois"    value={loading ? '…' : String(thisMonth.length)} icon={UserX}     iconBg="bg-purple-100" iconColor="text-purple-600" />
      </div>

      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-gray-700">Liste des Clients</h3>
          <div className="relative">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input value={search} onChange={e => setSearch(e.target.value)}
              placeholder="Rechercher…" className="form-input pl-9 w-64 text-sm" />
          </div>
        </div>
        {error && <p className="text-red-500 text-sm mb-4 bg-red-50 px-3 py-2 rounded-lg">{error}</p>}
        <table className="w-full text-sm">
          <thead>
            <tr className="text-xs text-gray-400 border-b border-gray-100">
              <th className="text-left pb-3">Nom</th>
              <th className="text-left pb-3">Email</th>
              <th className="text-left pb-3">Téléphone</th>
              <th className="text-left pb-3">Statut</th>
              <th className="text-left pb-3">Score crédit</th>
              <th className="text-left pb-3">Inscrit le</th>
              <th className="text-left pb-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="py-8 text-center text-gray-400">Chargement…</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={7} className="py-12 text-center text-gray-300">
                <Users size={32} className="mx-auto mb-2" />
                <p className="text-sm">{clients.length === 0 ? 'Aucun client dans cette agence' : 'Aucun résultat'}</p>
              </td></tr>
            ) : filtered.map(c => (
              <tr key={c.id} className="border-b border-gray-50 hover:bg-gray-50">
                <td className="py-3 font-medium text-gray-800">{c.firstName} {c.lastName}</td>
                <td className="py-3 text-gray-500">{c.email}</td>
                <td className="py-3 text-gray-600">{c.phoneNumber || '—'}</td>
                <td className="py-3"><Badge status={c.status?.toString().toLowerCase() || 'actif'} /></td>
                <td className="py-3 text-gray-600">{c.creditScore ?? '—'}</td>
                <td className="py-3 text-xs text-gray-400">
                  {c.createdAt ? new Date(c.createdAt).toLocaleDateString('fr-FR') : '—'}
                </td>
                <td className="py-3">
                  <div className="flex items-center gap-1">
                    <button onClick={() => navigate(`/directeur/clients/${c.id}`)} title="Voir détail"
                      className="p-1.5 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded-lg">
                      <Eye size={14} />
                    </button>
                    <button onClick={() => openEdit(c)} title="Modifier"
                      className="p-1.5 hover:bg-amber-50 text-gray-400 hover:text-amber-600 rounded-lg">
                      <Edit2 size={14} />
                    </button>
                    <button onClick={() => handleToggleStatus(c)}
                      title={c.status === 'ACTIVE' ? 'Rendre inactif' : 'Rendre actif'}
                      className={`p-1.5 rounded-lg ${c.status === 'ACTIVE' ? 'hover:bg-red-50 text-gray-400 hover:text-red-500' : 'hover:bg-green-50 text-gray-400 hover:text-green-600'}`}>
                      {c.status === 'ACTIVE' ? <ToggleRight size={14} /> : <ToggleLeft size={14} />}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Modal Nouveau Client */}
      <Modal isOpen={modalCreate} onClose={() => setModalCreate(false)} title="Nouveau Client">
        <form onSubmit={handleCreate} className="space-y-4 min-w-[420px]">
          <div className="grid grid-cols-2 gap-3">
            <div><label className="form-label">Prénom *</label>
              <input required value={form.firstName} onChange={e => setForm({...form, firstName: e.target.value})} className="form-input" /></div>
            <div><label className="form-label">Nom *</label>
              <input required value={form.lastName} onChange={e => setForm({...form, lastName: e.target.value})} className="form-input" /></div>
          </div>
          <div><label className="form-label">Email *</label>
            <input type="email" required value={form.email} onChange={e => setForm({...form, email: e.target.value})} className="form-input" /></div>
          <div className="grid grid-cols-2 gap-3">
            <div><label className="form-label">Téléphone</label>
              <input value={form.phoneNumber} onChange={e => setForm({...form, phoneNumber: e.target.value})} placeholder="+237..." className="form-input" /></div>
            <div><label className="form-label">Type</label>
              <select value={form.clientType} onChange={e => setForm({...form, clientType: e.target.value})} className="form-input">
                <option value="INDIVIDUAL">Individuel</option>
                <option value="ENTERPRISE">Entreprise</option>
              </select></div>
          </div>
          <div><label className="form-label">Adresse</label>
            <input value={form.address} onChange={e => setForm({...form, address: e.target.value})} className="form-input" /></div>
          {formError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{formError}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalCreate(false)} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={saving} className="btn-primary disabled:opacity-60">{saving ? 'Création…' : 'Créer le Client'}</button>
          </div>
        </form>
      </Modal>

      {/* Modal Modifier */}
      <Modal isOpen={!!modalEdit} onClose={() => setModalEdit(null)}
        title="Modifier le Client" subtitle={modalEdit ? `${modalEdit.firstName} ${modalEdit.lastName}` : ''}>
        <form onSubmit={handleEdit} className="space-y-4 min-w-[380px]">
          <div className="grid grid-cols-2 gap-3">
            <div><label className="form-label">Prénom *</label>
              <input required value={editForm.firstName} onChange={e => setEditForm({...editForm, firstName: e.target.value})} className="form-input" /></div>
            <div><label className="form-label">Nom *</label>
              <input required value={editForm.lastName} onChange={e => setEditForm({...editForm, lastName: e.target.value})} className="form-input" /></div>
          </div>
          <div><label className="form-label">Téléphone</label>
            <input value={editForm.phoneNumber} onChange={e => setEditForm({...editForm, phoneNumber: e.target.value})} placeholder="+237..." className="form-input" /></div>
          {formError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{formError}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalEdit(null)} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={saving} className="btn-primary disabled:opacity-60">{saving ? 'Enregistrement…' : 'Enregistrer'}</button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default DirecteurClientsPage;
