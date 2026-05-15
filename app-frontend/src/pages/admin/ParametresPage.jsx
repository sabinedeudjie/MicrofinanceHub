import React, { useState, useEffect, useCallback } from 'react';
import { Save, Settings, Percent, Tag, Shield, CheckCircle, AlertCircle, RefreshCw, UserPlus, Users, Pencil, Trash2 } from 'lucide-react';
import { getActiveLoanProducts, updateLoanProduct, createLoanProduct } from '../../api/loansApi';
import { registerAdmin, updateUser, deleteUser } from '../../api/authApi';
import authApiClient from '../../api/authApiClient';
import Modal from '../../components/common/Modal';
import { getCurrentUser } from '../../utils/auth';

const SUPER_ADMIN_EMAIL = 'admin@mfh.com';
const EMPTY_ADMIN_FORM = { nom: '', prenom: '', email: '', telephone: '', motDePasse: '' };
const EMPTY_EDIT_FORM  = { nom: '', prenom: '', telephone: '' };

const ParametresPage = () => {
  const isSuperAdmin = getCurrentUser()?.email === SUPER_ADMIN_EMAIL;
  const [produits,   setProduits]   = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [saving,     setSaving]     = useState(false);
  const [saved,      setSaved]      = useState(false);
  const [error,      setError]      = useState('');
  const [modifies,   setModifies]   = useState({});

  // Gestion des admins
  const [admins,          setAdmins]          = useState([]);
  const [adminsLoading,   setAdminsLoading]   = useState(true);
  const [modalAdmin,      setModalAdmin]      = useState(false);
  const [adminForm,       setAdminForm]       = useState(EMPTY_ADMIN_FORM);
  const [adminSaving,     setAdminSaving]     = useState(false);
  const [adminError,      setAdminError]      = useState('');

  const [modalEdit,       setModalEdit]       = useState(null);  // admin object being edited
  const [editForm,        setEditForm]        = useState(EMPTY_EDIT_FORM);
  const [editSaving,      setEditSaving]      = useState(false);
  const [editError,       setEditError]       = useState('');
  const [deletingId,      setDeletingId]      = useState(null);
  
  // Gestion des produits
  const [modalProduit,    setModalProduit]    = useState(false);
  const [produitForm,     setProduitForm]     = useState({
    name: '', description: '', minAmount: '', maxAmount: '', 
    minTermMonths: '', maxTermMonths: '', interestRate: ''
  });
  const [produitSaving,   setProduitSaving]   = useState(false);
  const [produitError,    setProduitError]    = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getActiveLoanProducts();
      const liste = Array.isArray(res.data) ? res.data : (res.data?.content ?? []);
      setProduits(liste);
      setModifies({});
    } catch {
      setError('Impossible de charger les produits de prêt. Vérifiez que loan-service est démarré (port 8083).');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const loadAdmins = useCallback(async () => {
    setAdminsLoading(true);
    try {
      const res = await authApiClient.get('/auth/users/by-role/ADMIN');
      setAdmins(Array.isArray(res.data) ? res.data : []);
    } catch { setAdmins([]); }
    finally { setAdminsLoading(false); }
  }, []);

  useEffect(() => { loadAdmins(); }, [loadAdmins]);

  const handleCreerAdmin = async (e) => {
    e.preventDefault();
    setAdminSaving(true);
    setAdminError('');
    const result = await registerAdmin(adminForm);
    if (!result.success) {
      setAdminError(result.error);
      setAdminSaving(false);
      return;
    }
    await loadAdmins();
    setModalAdmin(false);
    setAdminForm(EMPTY_ADMIN_FORM);
    setAdminSaving(false);
  };

  const openEdit = (admin) => {
    setEditForm({ nom: admin.lastName, prenom: admin.firstName, telephone: admin.phoneNumber || '' });
    setEditError('');
    setModalEdit(admin);
  };

  const handleModifierAdmin = async (e) => {
    e.preventDefault();
    setEditSaving(true);
    setEditError('');
    try {
      await updateUser(modalEdit.id, { firstName: editForm.prenom, lastName: editForm.nom, phoneNumber: editForm.telephone || null });
      await loadAdmins();
      setModalEdit(null);
    } catch (err) {
      setEditError(err.response?.data?.message ?? 'Erreur lors de la modification');
    } finally {
      setEditSaving(false);
    }
  };

  const handleSupprimerAdmin = async (admin) => {
    if (!window.confirm(`Supprimer l'administrateur ${admin.firstName} ${admin.lastName} ? Cette action est irréversible.`)) return;
    setDeletingId(admin.id);
    try {
      await deleteUser(admin.id);
      await loadAdmins();
    } catch (err) {
      alert(err.response?.data?.message ?? 'Erreur lors de la suppression');
    } finally {
      setDeletingId(null);
    }
  };

  const handleTauxChange = (id, value) => {
    setModifies(prev => ({ ...prev, [id]: { ...prev[id], interestRate: value } }));
  };

  const getValeur = (produit, champ) => {
    if (modifies[produit.id]?.[champ] !== undefined) return modifies[produit.id][champ];
    return produit[champ];
  };

  const handleSauvegarder = async () => {
    if (Object.keys(modifies).length === 0) {
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
      return;
    }
    setSaving(true);
    setError('');
    try {
      await Promise.all(
        Object.entries(modifies).map(([id, changes]) => {
          const original = produits.find(p => p.id === id);
          if (!original) return Promise.resolve();
          return updateLoanProduct(id, {
            name:         original.name,
            description:  original.description,
            minAmount:    original.minAmount,
            maxAmount:    original.maxAmount,
            minTermMonths: original.minTermMonths,
            maxTermMonths: original.maxTermMonths,
            interestRate: parseFloat(changes.interestRate ?? original.interestRate),
          });
        })
      );
      await load();
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch (err) {
      setError(err.response?.data?.message ?? 'Erreur lors de la sauvegarde');
    } finally {
      setSaving(false);
    }
  };

  const handleCreerProduit = async (e) => {
    e.preventDefault();
    setProduitSaving(true);
    setProduitError('');
    try {
      await createLoanProduct({
        ...produitForm,
        minAmount: parseFloat(produitForm.minAmount),
        maxAmount: parseFloat(produitForm.maxAmount),
        minTermMonths: parseInt(produitForm.minTermMonths),
        maxTermMonths: parseInt(produitForm.maxTermMonths),
        interestRate: parseFloat(produitForm.interestRate),
      });
      await load();
      setModalProduit(false);
      setProduitForm({
        name: '', description: '', minAmount: '', maxAmount: '', 
        minTermMonths: '', maxTermMonths: '', interestRate: ''
      });
    } catch (err) {
      setProduitError(err.response?.data?.message ?? 'Erreur lors de la création du produit');
    } finally {
      setProduitSaving(false);
    }
  };

  const nbModifies = Object.keys(modifies).length;

  return (
    <div className="p-6 space-y-6">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Paramètres</h1>
          <p className="text-gray-500 text-sm">Configuration générale de l'application MicroFinanceHub</p>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={load} disabled={loading} className="btn-secondary flex items-center gap-2 text-sm">
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} /> Actualiser
          </button>
          {saved && (
            <span className="flex items-center gap-1.5 text-green-600 text-sm font-medium">
              <CheckCircle size={16} /> Paramètres sauvegardés
            </span>
          )}
          <button onClick={handleSauvegarder} disabled={saving} className="btn-primary flex items-center gap-2 disabled:opacity-60">
            <Save size={15} /> {saving ? 'Sauvegarde…' : `Sauvegarder${nbModifies > 0 ? ` (${nbModifies})` : ''}`}
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm flex items-center gap-2">
          <AlertCircle size={16} /> {error}
        </div>
      )}

      {/* Taux d'intérêt par produit de prêt */}
      <div className="card">
        <h3 className="font-semibold text-gray-700 flex items-center gap-2 mb-4">
          <Percent size={16} className="text-blue-500" /> Taux d'Intérêt par Produit de Prêt
          <button 
            onClick={() => { setProduitError(''); setModalProduit(true); }}
            className="ml-auto text-xs bg-blue-50 text-blue-600 px-2 py-1 rounded hover:bg-blue-100 flex items-center gap-1"
          >
            <Tag size={12} /> Nouveau Produit
          </button>
        </h3>
        {loading ? (
          <p className="text-gray-400 text-sm text-center py-6">Chargement des produits…</p>
        ) : produits.length === 0 ? (
          <p className="text-gray-400 text-sm text-center py-6">
            Aucun produit actif. Créez des produits de prêt via l'API loan-service.
          </p>
        ) : (
          <div className="space-y-3">
            {produits.map(p => (
              <div key={p.id} className="flex items-center gap-4 bg-gray-50 rounded-xl p-4">
                <div className="flex-1">
                  <p className="font-medium text-gray-700">{p.name}</p>
                  <p className="text-xs text-gray-400">
                    {p.description || `${(p.minAmount ?? 0).toLocaleString('fr-FR')} – ${(p.maxAmount ?? 0).toLocaleString('fr-FR')} FCFA · ${p.minTermMonths}–${p.maxTermMonths} mois`}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <input
                    type="number" step="0.1" min="0" max="30"
                    value={getValeur(p, 'interestRate') ?? ''}
                    onChange={e => handleTauxChange(p.id, e.target.value)}
                    className={`form-input w-24 text-right font-semibold ${modifies[p.id] ? 'border-blue-400 bg-blue-50' : ''}`}
                  />
                  <span className="text-gray-500 font-medium">%</span>
                  {modifies[p.id] && <span className="text-xs text-blue-500">modifié</span>}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Résumé des paramètres (calculé à partir des produits) */}
      {produits.length > 0 && (
        <div className="card">
          <h3 className="font-semibold text-gray-700 flex items-center gap-2 mb-4">
            <Settings size={16} className="text-blue-500" /> Résumé des Paramètres Produits
          </h3>
          <div className="grid grid-cols-3 gap-4 text-sm">
            {[
              ['Montant Min global',  Math.min(...produits.map(p => Number(p.minAmount ?? 0))).toLocaleString('fr-FR') + ' FCFA'],
              ['Montant Max global',  Math.max(...produits.map(p => Number(p.maxAmount ?? 0))).toLocaleString('fr-FR') + ' FCFA'],
              ['Durée Min globale',   Math.min(...produits.map(p => p.minTermMonths ?? 999)) + ' mois'],
              ['Durée Max globale',   Math.max(...produits.map(p => p.maxTermMonths ?? 0)) + ' mois'],
              ['Taux Min',            Math.min(...produits.map(p => Number(p.interestRate ?? 0))).toFixed(1) + '%'],
              ['Taux Max',            Math.max(...produits.map(p => Number(p.interestRate ?? 0))).toFixed(1) + '%'],
            ].map(([label, val]) => (
              <div key={label} className="flex justify-between border-b border-gray-50 pb-2">
                <span className="text-gray-500">{label}</span>
                <span className="font-semibold text-gray-800">{val}</span>
              </div>
            ))}
          </div>
          <p className="text-xs text-gray-400 mt-3">
            Ces valeurs sont calculées à partir des produits actifs. Pour modifier, ajustez les produits via le tableau ci-dessus.
          </p>
        </div>
      )}

      {/* Administrateurs — visible uniquement pour le super admin */}
      {isSuperAdmin && <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-gray-700 flex items-center gap-2">
            <Users size={16} className="text-blue-500" /> Administrateurs
          </h3>
          <button onClick={() => { setAdminForm(EMPTY_ADMIN_FORM); setAdminError(''); setModalAdmin(true); }}
            className="btn-primary flex items-center gap-2 text-sm">
            <UserPlus size={14} /> Nouvel Admin
          </button>
        </div>
        {adminsLoading ? (
          <p className="text-gray-400 text-sm text-center py-4">Chargement…</p>
        ) : admins.length === 0 ? (
          <p className="text-gray-400 text-sm text-center py-4">Aucun administrateur trouvé.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-400 border-b border-gray-100">
                <th className="text-left pb-2">Nom</th>
                <th className="text-left pb-2">Email</th>
                <th className="text-left pb-2">Téléphone</th>
                <th className="text-left pb-2">Créé le</th>
                <th className="text-left pb-2">Actions</th>
              </tr>
            </thead>
            <tbody>
              {admins.map(a => (
                <tr key={a.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="py-2 font-medium text-gray-800">{a.firstName} {a.lastName}</td>
                  <td className="py-2 text-gray-500 text-xs">{a.email}</td>
                  <td className="py-2 text-gray-600">{a.phoneNumber || '—'}</td>
                  <td className="py-2 text-xs text-gray-400">
                    {a.createdAt ? new Date(a.createdAt).toLocaleDateString('fr-FR') : '—'}
                  </td>
                  <td className="py-2">
                    {a.email !== SUPER_ADMIN_EMAIL && (
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => openEdit(a)}
                          className="p-1.5 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded-lg"
                          title="Modifier"
                        >
                          <Pencil size={14} />
                        </button>
                        <button
                          onClick={() => handleSupprimerAdmin(a)}
                          disabled={deletingId === a.id}
                          className="p-1.5 hover:bg-red-50 text-gray-400 hover:text-red-500 rounded-lg disabled:opacity-50"
                          title="Supprimer"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>}

      {/* Seuils de solvabilité */}
      <div className="card">
        <h3 className="font-semibold text-gray-700 flex items-center gap-2 mb-4">
          <Shield size={16} className="text-blue-500" /> Seuils de Solvabilité (score /100)
        </h3>
        <p className="text-xs text-gray-400 mb-4">
          Référence interne — le score minimum par produit est configurable via le champ <code>minCreditScore</code> de chaque produit.
        </p>
        <div className="grid grid-cols-3 gap-4">
          {[
            { label: 'Excellent (≥)',  valeur: 80, color: 'text-green-600', bg: 'bg-green-50' },
            { label: 'Bonne (≥)',      valeur: 60, color: 'text-blue-600',  bg: 'bg-blue-50' },
            { label: 'Moyenne (≥)',    valeur: 40, color: 'text-yellow-600',bg: 'bg-yellow-50' },
          ].map(s => (
            <div key={s.label} className={`${s.bg} rounded-xl p-4 text-center`}>
              <p className={`font-semibold ${s.color} mb-1`}>{s.label}</p>
              <p className={`text-3xl font-bold ${s.color}`}>{s.valeur}</p>
              <p className="text-xs text-gray-500 mt-1">/100</p>
            </div>
          ))}
        </div>
        <p className="text-xs text-gray-400 mt-3">En dessous du seuil "Moyenne", le client est classé "Faible".</p>
      </div>

      {/* Types de prêts actifs */}
      {produits.length > 0 && (
        <div className="card">
          <h3 className="font-semibold text-gray-700 flex items-center gap-2 mb-4">
            <Tag size={16} className="text-blue-500" /> Produits de Prêts Actifs
          </h3>
          <div className="flex flex-wrap gap-2">
            {produits.map(p => (
              <span key={p.id} className="bg-blue-100 text-blue-700 px-3 py-1.5 rounded-full text-sm font-medium">
                {p.name} — {Number(getValeur(p, 'interestRate')).toFixed(1)}%
              </span>
            ))}
          </div>
        </div>
      )}
      {/* Modal Modifier un Admin */}
      <Modal isOpen={!!modalEdit} onClose={() => { setModalEdit(null); setEditError(''); }}
        title="Modifier l'Administrateur"
        subtitle={modalEdit ? `${modalEdit.firstName} ${modalEdit.lastName} — ${modalEdit.email}` : ''}>
        <form onSubmit={handleModifierAdmin} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Nom *</label>
              <input value={editForm.nom} onChange={e => setEditForm(p => ({ ...p, nom: e.target.value }))}
                required className="form-input" />
            </div>
            <div>
              <label className="form-label">Prénom *</label>
              <input value={editForm.prenom} onChange={e => setEditForm(p => ({ ...p, prenom: e.target.value }))}
                required className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Téléphone</label>
            <input value={editForm.telephone} onChange={e => setEditForm(p => ({ ...p, telephone: e.target.value }))}
              placeholder="+237 6XX XXX XXX" className="form-input" />
          </div>
          {editError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{editError}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalEdit(null); setEditError(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={editSaving} className="btn-primary disabled:opacity-60">
              {editSaving ? 'Sauvegarde…' : 'Enregistrer'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal Créer un Admin — super admin uniquement */}
      <Modal isOpen={isSuperAdmin && modalAdmin} onClose={() => { setModalAdmin(false); setAdminError(''); }}
        title="Créer un Administrateur"
        subtitle="Le compte aura accès complet à l'administration">
        <form onSubmit={handleCreerAdmin} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Nom *</label>
              <input value={adminForm.nom} onChange={e => setAdminForm(p => ({ ...p, nom: e.target.value }))}
                placeholder="Ex: Fotso" required className="form-input" />
            </div>
            <div>
              <label className="form-label">Prénom *</label>
              <input value={adminForm.prenom} onChange={e => setAdminForm(p => ({ ...p, prenom: e.target.value }))}
                placeholder="Ex: Paul" required className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Email *</label>
            <input type="email" value={adminForm.email} onChange={e => setAdminForm(p => ({ ...p, email: e.target.value }))}
              placeholder="admin@mfh.com" required className="form-input" />
          </div>
          <div>
            <label className="form-label">Téléphone</label>
            <input value={adminForm.telephone} onChange={e => setAdminForm(p => ({ ...p, telephone: e.target.value }))}
              placeholder="+237 6XX XXX XXX" className="form-input" />
          </div>
          <div>
            <label className="form-label">Mot de Passe Provisoire *</label>
            <input type="password" value={adminForm.motDePasse}
              onChange={e => setAdminForm(p => ({ ...p, motDePasse: e.target.value }))}
              placeholder="Minimum 6 caractères" required minLength={6} className="form-input" />
          </div>
          {adminError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{adminError}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalAdmin(false); setAdminError(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={adminSaving} className="btn-primary disabled:opacity-60">
              {adminSaving ? 'Création...' : "Créer l'Admin"}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal Créer un Produit de Prêt */}
      <Modal isOpen={modalProduit} onClose={() => setModalProduit(false)}
        title="Nouveau Produit de Prêt"
        subtitle="Définissez les conditions du nouveau produit financier">
        <form onSubmit={handleCreerProduit} className="space-y-4">
          <div>
            <label className="form-label">Nom du Produit *</label>
            <input value={produitForm.name} onChange={e => setProduitForm(p => ({ ...p, name: e.target.value }))}
              placeholder="Ex: Prêt d'Urgence" required className="form-input" />
          </div>
          <div>
            <label className="form-label">Description</label>
            <textarea value={produitForm.description} onChange={e => setProduitForm(p => ({ ...p, description: e.target.value }))}
              placeholder="Description courte du produit..." className="form-input" rows="2" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Montant Min (FCFA) *</label>
              <input type="number" value={produitForm.minAmount} onChange={e => setProduitForm(p => ({ ...p, minAmount: e.target.value }))}
                required className="form-input" />
            </div>
            <div>
              <label className="form-label">Montant Max (FCFA) *</label>
              <input type="number" value={produitForm.maxAmount} onChange={e => setProduitForm(p => ({ ...p, maxAmount: e.target.value }))}
                required className="form-input" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Durée Min (mois) *</label>
              <input type="number" value={produitForm.minTermMonths} onChange={e => setProduitForm(p => ({ ...p, minTermMonths: e.target.value }))}
                required className="form-input" />
            </div>
            <div>
              <label className="form-label">Durée Max (mois) *</label>
              <input type="number" value={produitForm.maxTermMonths} onChange={e => setProduitForm(p => ({ ...p, maxTermMonths: e.target.value }))}
                required className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Taux d'intérêt (%) *</label>
            <input type="number" step="0.1" value={produitForm.interestRate} onChange={e => setProduitForm(p => ({ ...p, interestRate: e.target.value }))}
              required className="form-input" />
          </div>
          {produitError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{produitError}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalProduit(false)} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={produitSaving} className="btn-primary disabled:opacity-60">
              {produitSaving ? 'Création...' : "Créer le Produit"}
            </button>
          </div>
        </form>
      </Modal>

    </div>
  );
};

export default ParametresPage;
