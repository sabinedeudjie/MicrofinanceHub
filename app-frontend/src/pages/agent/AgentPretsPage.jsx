import React, { useState, useEffect, useCallback } from 'react';
import { Plus, FileText } from 'lucide-react';
import Modal from '../../components/common/Modal';
import { getCurrentUser } from '../../utils/auth';
import { getMyClients } from '../../api/clientsApi';
import { getLoansByClients, getApplicationsByClients, applyForLoan } from '../../api/loansApi';
import { getComptesActifsByClient } from '../../api/comptesApi';

const fmt = (n) => n != null ? Number(n).toLocaleString('fr-FR') : '—';

const STATUS_LABELS = {
  PENDING: 'En attente', APPROVED: 'Approuvé', REJECTED: 'Rejeté',
  DISBURSED: 'Décaissé', ACTIVE: 'Actif', COMPLETED: 'Terminé',
};

const TYPES = ['Agriculture', 'Commerce', 'Artisanat', 'Éducation', 'Santé', 'Autre'];

const AgentPretsPage = () => {
  const user = getCurrentUser();

  const [loans,    setLoans]    = useState([]);
  const [clients,  setClients]  = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [modal,    setModal]    = useState(false);
  const [saving,   setSaving]   = useState(false);
  const [erreur,   setErreur]   = useState('');
  const [form, setForm] = useState({
    clientId: '', requestedAmount: '', termMonths: '', loanType: '', purpose: '', accountNumber: '',
  });
  const [comptes, setComptes] = useState([]);
  const [loadingComptes, setLoadingComptes] = useState(false);

  const load = useCallback(async () => {
    if (!user?.id) return;
    setLoading(true);
    try {
      const clientsRes = await getMyClients().catch(() => null);
      const myClients = clientsRes?.data?.content ?? clientsRes?.data ?? [];
      setClients(myClients);

      if (myClients.length > 0) {
        const clientIds = myClients.map(c => c.id);
        
        // Charger parallèlement les prêts et les demandes
        const [loansRes, appsRes] = await Promise.all([
          getLoansByClients(clientIds).catch(() => ({ data: [] })),
          getApplicationsByClients(clientIds).catch(() => ({ data: [] }))
        ]);

        const loansList = loansRes.data?.content ?? loansRes.data ?? [];
        const appsList  = appsRes.data?.content ?? appsRes.data ?? [];

        // On fusionne les deux listes. 
        // Pour éviter les doublons, on ne prend les "demandes" que si elles sont PENDING ou REJECTED
        // car les APPROVED/DISBURSED sont déjà dans loansList.
        const filteredApps = appsList.filter(app => 
          app.status === 'PENDING' || app.status === 'REJECTED'
        );

        // On normalise les objets pour qu'ils soient affichables dans la même table
        const unifiedList = [
          ...loansList,
          ...filteredApps
        ].sort((a, b) => {
          const dateA = new Date(a.createdAt || a.applicationDate || 0);
          const dateB = new Date(b.createdAt || b.applicationDate || 0);
          return dateB - dateA; // Plus récent en premier
        });

        setLoans(unifiedList);
      } else {
        setLoans([]);
      }
    } catch (err) {
      console.error("Erreur chargement prêts agent", err);
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  useEffect(() => { load(); }, [load]);

  // Charger les comptes du client sélectionné
  useEffect(() => {
    if (!form.clientId) {
      setComptes([]);
      return;
    }
    const fetchComptes = async () => {
      setLoadingComptes(true);
      try {
        const res = await getComptesActifsByClient(form.clientId);
        const data = res.data?.data?.content ?? res.data?.data ?? res.data ?? [];
        setComptes(Array.isArray(data) ? data : []);
      } catch {
        setComptes([]);
      } finally {
        setLoadingComptes(false);
      }
    };
    fetchComptes();
  }, [form.clientId]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErreur('');
    setSaving(true);
    try {
      await applyForLoan({
        clientId: form.clientId,
        accountNumber: form.accountNumber || '', // S'assurer que le numéro de compte est présent
        requestedAmount: parseFloat(form.requestedAmount),
        termMonths: parseInt(form.termMonths),
        purpose: form.purpose ? `[${form.loanType}] ${form.purpose}` : form.loanType,
      });
      await load();
      setModal(false);
      setForm({ clientId: '', requestedAmount: '', termMonths: '', loanType: '', purpose: '', accountNumber: '' });
    } catch (err) {
      setErreur(err.response?.data?.message ?? 'Erreur lors de la soumission');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="p-6 space-y-5">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Demandes de Prêt</h1>
          <p className="text-gray-500 text-sm">Soumettez et suivez les demandes de prêt de vos clients</p>
        </div>
        <button onClick={() => setModal(true)} className="btn-primary flex items-center gap-2">
          <Plus size={16} /> Nouvelle Demande
        </button>
      </div>

      <div className="bg-blue-50 border border-blue-100 rounded-xl p-4 text-sm text-blue-700">
        <strong>Note :</strong> L'approbation, le rejet et le décaissement sont traités par l'administration.
      </div>

      <div className="card">
        <h3 className="font-semibold text-gray-700 mb-1">Prêts de Mon Portefeuille</h3>
        <p className="text-xs text-gray-400 mb-4">
          {loading ? 'Chargement...' : `${loans.length} prêt(s) enregistré(s)`}
        </p>
        <div className="table-container">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-400 border-b border-gray-100">
                <th className="text-left pb-3">Référence</th>
                <th className="text-left pb-3">Client</th>
                <th className="text-left pb-3">Montant</th>
                <th className="text-left pb-3">Durée</th>
                <th className="text-left pb-3">Mensualité</th>
                <th className="text-left pb-3">Statut</th>
                <th className="text-left pb-3">Date</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={7} className="py-8 text-center text-gray-400">Chargement...</td></tr>
              ) : loans.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-gray-300">
                    <FileText size={32} className="mx-auto mb-2" />
                    <p className="text-sm">Aucune demande de prêt</p>
                  </td>
                </tr>
              ) : (
                loans.map(p => (
                  <tr key={p.id} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 font-mono text-xs text-blue-700">{p.loanNumber ?? p.applicationNumber ?? p.id?.slice(0, 8)}</td>
                    <td className="py-3 font-medium text-gray-800">{p.clientFirstName} {p.clientLastName}</td>
                    <td className="py-3">{fmt(p.amount ?? p.requestedAmount)} FCFA</td>
                    <td className="py-3">{p.termMonths} mois</td>
                    <td className="py-3">{p.monthlyPayment ? fmt(p.monthlyPayment) + ' FCFA' : '—'}</td>
                    <td className="py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full ${
                        p.status === 'ACTIVE' ? 'bg-green-100 text-green-700' :
                        p.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' :
                        p.status === 'REJECTED' ? 'bg-red-100 text-red-600' :
                        'bg-gray-100 text-gray-600'
                      }`}>
                        {STATUS_LABELS[p.status] ?? p.status}
                      </span>
                    </td>
                    <td className="py-3 text-xs text-gray-400">
                      {p.applicationDate || p.disbursementDate
                        ? new Date(p.applicationDate ?? p.disbursementDate).toLocaleDateString('fr-FR')
                        : '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <Modal isOpen={modal} onClose={() => { setModal(false); setErreur(''); }}
        title="Nouvelle Demande de Prêt" subtitle="Soumettez une demande pour un de vos clients">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="form-label">Client *</label>
            <select value={form.clientId} onChange={e => setForm(p => ({ ...p, clientId: e.target.value, accountNumber: '' }))}
              required className="form-input">
              <option value="">— Choisir un client —</option>
              {clients.map(c => (
                <option key={c.id} value={c.id}>{c.firstName} {c.lastName} ({c.email})</option>
              ))}
            </select>
          </div>

          {form.clientId && (
            <div className="animate-in fade-in slide-in-from-top-1 duration-200">
              <label className="form-label">Compte du client *</label>
              <select value={form.accountNumber} onChange={e => setForm(p => ({ ...p, accountNumber: e.target.value }))}
                required className="form-input">
                <option value="">— Choisir le compte de réception —</option>
                {loadingComptes ? (
                  <option disabled>Chargement des comptes...</option>
                ) : comptes.length === 0 ? (
                  <option disabled>Aucun compte actif trouvé</option>
                ) : (
                  comptes.map(c => (
                    <option key={c.id} value={c.numeroCompte}>
                      {c.numeroCompte} — {c.typeCompte} ({fmt(c.solde)} FCFA)
                    </option>
                  ))
                )}
              </select>
            </div>
          )}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Montant (FCFA) *</label>
              <input type="number" value={form.requestedAmount}
                onChange={e => setForm(p => ({ ...p, requestedAmount: e.target.value }))}
                placeholder="Ex: 500000" required min="10000" className="form-input" />
            </div>
            <div>
              <label className="form-label">Durée (mois) *</label>
              <input type="number" value={form.termMonths}
                onChange={e => setForm(p => ({ ...p, termMonths: e.target.value }))}
                placeholder="Ex: 12" required min="3" max="60" className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Type de Prêt *</label>
            <select value={form.loanType} onChange={e => setForm(p => ({ ...p, loanType: e.target.value }))}
              required className="form-input">
              <option value="">Sélectionner</option>
              {TYPES.map(t => <option key={t}>{t}</option>)}
            </select>
          </div>
          <div>
            <label className="form-label">Objet du Prêt *</label>
            <textarea rows={3} value={form.purpose}
              onChange={e => setForm(p => ({ ...p, purpose: e.target.value }))}
              placeholder="Décrivez l'utilisation prévue..." required className="form-input resize-none" />
          </div>
          {erreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{erreur}</p>}
          <div className="flex justify-end gap-3">
            <button type="button" onClick={() => { setModal(false); setErreur(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={saving} className="btn-primary disabled:opacity-60">
              {saving ? 'Soumission...' : 'Soumettre la Demande'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default AgentPretsPage;
