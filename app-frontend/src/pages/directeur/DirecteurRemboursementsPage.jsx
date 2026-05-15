import React, { useState, useEffect, useCallback } from 'react';
import {
  CheckCircle, Calendar, DollarSign, AlertTriangle, Banknote, ChevronDown, ChevronUp, CreditCard,
} from 'lucide-react';
import Modal from '../../components/common/Modal';
import StatCard from '../../components/common/StatCard';
import { getMyAgencyClients } from '../../api/agencyApi';
import { getLoansByClients, getAmortizationSchedule } from '../../api/loansApi';
import {
  agentRecordPayment,
  getRepaymentStatsForClients,
  getTotalRepaymentsForClients,
} from '../../api/repaymentApi';
import { getComptesActifsByClient } from '../../api/comptesApi';

const fmt = (n) => (n != null ? Number(n).toLocaleString('fr-FR') : '—');
const fmtPct = (n) => (n != null ? `${Number(n).toFixed(1)} %` : '—');

const PAYMENT_METHODS = [
  { value: 'CASH',          label: 'Espèces' },
  { value: 'MOBILE_MONEY',  label: 'Mobile Money' },
  { value: 'BANK_TRANSFER', label: 'Virement bancaire' },
  { value: 'CHECK',         label: 'Chèque' },
];

const DirecteurRemboursementsPage = () => {
  const [loans,        setLoans]        = useState([]);
  const [stats,        setStats]        = useState(null);
  const [totalCollect, setTotalCollect] = useState(null);
  const [pendingPayments, setPendingPayments] = useState([]);
  const [loading,      setLoading]      = useState(true);
  const [loadingPending, setLoadingPending] = useState(false);
  const [expandedLoan, setExpandedLoan] = useState(null);
  const [amortCache,   setAmortCache]   = useState({});

  const [loadError,    setLoadError]    = useState('');
  const [modal,        setModal]        = useState(null);
  const [amortModal,   setAmortModal]   = useState(null);
  const [montant,      setMontant]      = useState('');
  const [modePaiement, setModePaiement] = useState('CASH');
  const [numRecu,      setNumRecu]      = useState('');
  const [numeroPaiement, setNumeroPaiement] = useState('');
  const [compteSourceId, setCompteSourceId] = useState('');
  const [clientComptes,  setClientComptes]  = useState([]);
  const [saving,       setSaving]       = useState(false);
  const [validating,   setValidating]   = useState(null);
  const [erreur,       setErreur]       = useState('');
  const [successId,    setSuccessId]    = useState(null);
  const [validSuccess, setValidSuccess] = useState(null);

  const loadPending = useCallback(async () => {
    setLoadingPending(true);
    try {
      const { getPendingPayments } = await import('../../api/repaymentApi');
      const res = await getPendingPayments();
      // Filtrer par agence si possible côté backend ou frontend ? 
      // Pour l'instant on affiche tout, le backend vérifiera les droits lors de la validation.
      setPendingPayments(res.data ?? []);
    } catch (err) {
      console.error("Erreur chargement paiements en attente", err);
    } finally {
      setLoadingPending(false);
    }
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError('');
    try {
      const agencyRes = await getMyAgencyClients();
      const ids = (agencyRes.data?.clients ?? []).map(c => c.clientId).filter(Boolean);
      if (ids.length === 0) { setLoading(false); return; }

      const [loansRes, statsRes, totalRes] = await Promise.allSettled([
        getLoansByClients(ids, 0, 200, 'ACTIVE'),
        getRepaymentStatsForClients(ids),
        getTotalRepaymentsForClients(ids),
      ]);
      if (loansRes.status === 'fulfilled') {
        const data = loansRes.value.data;
        setLoans(data?.content ?? (Array.isArray(data) ? data : []));
      }
      if (statsRes.status === 'fulfilled') setStats(statsRes.value.data);
      if (totalRes.status === 'fulfilled') setTotalCollect(totalRes.value.data);
    } catch (err) {
      const msg = err.response?.data?.message ?? err.response?.data?.error ?? 'Impossible de charger les données de remboursement.';
      setLoadError(String(msg));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { loadPending(); }, [loadPending]);

  // Charger les comptes du client si virement bancaire
  useEffect(() => {
    if (modal && modePaiement === 'BANK_TRANSFER') {
      const fetchComptes = async () => {
        try {
          const res = await getComptesActifsByClient(modal.clientId);
          setClientComptes(res.data?.content ?? res.data ?? []);
        } catch (err) {
          console.error("Erreur chargement comptes", err);
          setClientComptes([]);
        }
      };
      fetchComptes();
    }
  }, [modal, modePaiement]);

  const handleValidate = async (paymentId) => {
    setValidating(paymentId);
    try {
      const { validatePayment } = await import('../../api/repaymentApi');
      await validatePayment(paymentId);
      setValidSuccess(paymentId);
      setTimeout(() => setValidSuccess(null), 3000);
      await Promise.all([loadPending(), load()]);
    } catch (err) {
      alert("Erreur lors de la validation: " + (err.response?.data?.message ?? "Erreur technique"));
    } finally {
      setValidating(null);
    }
  };

  const loadAmort = async (loanId) => {
    if (amortCache[loanId]) return;
    try {
      const res = await getAmortizationSchedule(loanId);
      const entries = res.data?.entries ?? res.data?.schedule ?? res.data ?? [];
      setAmortCache(prev => ({ ...prev, [loanId]: entries }));
    } catch {
      setAmortCache(prev => ({ ...prev, [loanId]: [] }));
    }
  };

  const toggleExpand = async (loanId) => {
    if (expandedLoan === loanId) { setExpandedLoan(null); return; }
    setExpandedLoan(loanId);
    await loadAmort(loanId);
  };

  const openModal = async (loan) => {
    setModal(loan);
    setErreur('');
    setNumRecu('');
    setNumeroPaiement('');
    setCompteSourceId('');
    setModePaiement('CASH');
    setAmortModal(null);
    await loadAmort(loan.id);
    const entries = amortCache[loan.id] ?? [];
    const next = entries.find(e => !e.paid);
    setMontant(String(next?.dueAmount ?? loan.monthlyPayment ?? ''));
    setAmortModal(entries);
  };

  useEffect(() => {
    if (modal && amortCache[modal.id]) setAmortModal(amortCache[modal.id]);
  }, [amortCache, modal]);

  const handleEnregistrer = async (e) => {
    e.preventDefault();
    if (!modal) return;
    setSaving(true);
    setErreur('');
    try {
      const amount = parseFloat(montant);
      if (isNaN(amount) || amount <= 0) { setErreur('Montant invalide.'); setSaving(false); return; }
      await agentRecordPayment({
        loanId:        modal.id,
        clientId:      modal.clientId,
        amount,
        paymentMethod: modePaiement,
        numeroPaiement: modePaiement === 'MOBILE_MONEY' ? numeroPaiement : undefined,
        compteSourceId: modePaiement === 'BANK_TRANSFER' ? (compteSourceId || undefined) : undefined,
        receiptNumber: numRecu || undefined,
      });
      setSuccessId(modal.id);
      setModal(null);
      setAmortCache(prev => { const n = { ...prev }; delete n[modal.id]; return n; });
      await load();
      setTimeout(() => setSuccessId(null), 4000);
    } catch (err) {
      setErreur(String(err.response?.data?.message ?? err.response?.data ?? 'Erreur'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="p-6 space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Remboursements</h1>
        <p className="text-gray-500 text-sm">Prêts actifs de votre agence et enregistrement des paiements</p>
      </div>

      {loadError && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm">
          {loadError}
        </div>
      )}

      <div className="grid grid-cols-4 gap-4">
        <StatCard title="Prêts Actifs"         value={loading ? '…' : String(loans.length)}       icon={Calendar}      iconBg="bg-blue-100"   iconColor="text-blue-600" />
        <StatCard title="Collecté ce mois (FCFA)" value={loading ? '…' : fmt(totalCollect)}         icon={DollarSign}    iconBg="bg-green-100"  iconColor="text-green-600" />
        <StatCard title="En Retard (FCFA)"      value={loading ? '…' : fmt(stats?.overdueAmount)} icon={AlertTriangle} iconBg="bg-red-100"    iconColor="text-red-500"
          subtitle={stats ? `${fmt(stats.overdueCount)} dossiers` : undefined} />
        <StatCard title="Taux Recouvrement"     value={loading ? '…' : fmtPct(stats?.repaymentRate)} icon={CheckCircle} iconBg="bg-purple-100" iconColor="text-purple-600" />
      </div>

      <div className="card">
        <h3 className="font-semibold text-gray-700 mb-1">Prêts Actifs — Agence</h3>
        <p className="text-xs text-gray-400 mb-4">{loading ? 'Chargement…' : `${loans.length} prêt(s) actif(s)`}</p>

        {loading ? (
          <div className="py-12 text-center text-gray-400">Chargement…</div>
        ) : loans.length === 0 ? (
          <div className="py-12 text-center text-gray-300">
            <Calendar size={36} className="mx-auto mb-2" />
            <p className="text-sm">Aucun prêt actif dans votre agence</p>
          </div>
        ) : (
          <div className="space-y-2">
            {loans.map(loan => {
              const expanded = expandedLoan === loan.id;
              const entries  = amortCache[loan.id];
              const paid     = entries ? entries.filter(e => e.paid).length  : null;
              const total    = entries ? entries.length : null;
              const pct      = paid != null && total ? Math.round((paid / total) * 100) : null;

              return (
                <div key={loan.id} className={`border rounded-xl ${successId === loan.id ? 'border-green-300 bg-green-50' : 'border-gray-100 bg-white'}`}>
                  <div className="flex items-center gap-4 p-4">
                    <div className="w-9 h-9 bg-emerald-100 rounded-xl flex items-center justify-center shrink-0">
                      <CreditCard size={16} className="text-emerald-700" />
                    </div>
                    <div className="flex-1 min-w-0 grid grid-cols-5 gap-3 items-center text-sm">
                      <div>
                        <p className="font-medium text-gray-800 truncate">{loan.clientFirstName} {loan.clientLastName}</p>
                        <p className="text-xs text-gray-400 font-mono">{loan.loanNumber ?? loan.id?.slice(0, 10)}</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-400">Montant prêt</p>
                        <p className="font-semibold text-gray-700">{fmt(loan.amount)} FCFA</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-400">Mensualité</p>
                        <p className="font-bold text-emerald-700">{fmt(loan.monthlyPayment)} FCFA</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-400">Restant dû</p>
                        <p className="font-semibold">{fmt(loan.remainingBalance)} FCFA</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-400">Prochain paiement</p>
                        <p className="text-gray-700 text-xs">
                          {loan.nextPaymentDate ? new Date(loan.nextPaymentDate).toLocaleDateString('fr-FR') : '—'}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      {successId === loan.id ? (
                        <span className="flex items-center gap-1 text-green-600 text-sm font-medium">
                          <CheckCircle size={15} /> Enregistré !
                        </span>
                      ) : (
                        <button onClick={() => openModal(loan)} className="bg-emerald-700 hover:bg-emerald-800 text-white text-xs px-3 py-1.5 rounded-lg font-medium flex items-center gap-1.5">
                          <Banknote size={13} /> Paiement
                        </button>
                      )}
                      <button onClick={() => toggleExpand(loan.id)} className="p-1.5 hover:bg-gray-100 rounded-lg text-gray-400">
                        {expanded ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
                      </button>
                    </div>
                  </div>

                  {pct !== null && (
                    <div className="px-4 pb-2">
                      <div className="flex justify-between text-xs text-gray-400 mb-1">
                        <span>{paid}/{total} échéances payées</span><span>{pct}%</span>
                      </div>
                      <div className="h-1.5 bg-gray-100 rounded-full">
                        <div className={`h-1.5 rounded-full transition-all ${pct >= 100 ? 'bg-green-500' : 'bg-emerald-600'}`}
                          style={{ width: `${Math.min(pct, 100)}%` }} />
                      </div>
                    </div>
                  )}

                  {expanded && (
                    <div className="border-t border-gray-100 px-4 pb-4 pt-3">
                      {!entries ? (
                        <p className="text-center text-gray-400 text-sm py-4">Chargement…</p>
                      ) : (
                        <div className="overflow-auto max-h-52">
                          <table className="w-full text-xs">
                            <thead>
                              <tr className="text-gray-400 border-b border-gray-100">
                                <th className="text-left pb-2">Éch.</th>
                                <th className="text-left pb-2">Date</th>
                                <th className="text-right pb-2">Montant dû</th>
                                <th className="text-right pb-2">Capital</th>
                                <th className="text-right pb-2">Intérêts</th>
                                <th className="text-right pb-2">Solde restant</th>
                                <th className="text-center pb-2">Statut</th>
                              </tr>
                            </thead>
                            <tbody>
                              {entries.map((e, i) => (
                                <tr key={i} className={`border-b border-gray-50 ${e.paid ? 'opacity-50' : ''}`}>
                                  <td className="py-1.5 font-medium">{e.installmentNumber ?? i + 1}</td>
                                  <td className="py-1.5 text-gray-500">{e.dueDate ? new Date(e.dueDate).toLocaleDateString('fr-FR') : '—'}</td>
                                  <td className="py-1.5 text-right font-semibold">{fmt(e.dueAmount ?? e.monthlyPayment)} FCFA</td>
                                  <td className="py-1.5 text-right text-green-600">{fmt(e.principalAmount ?? e.principal)} FCFA</td>
                                  <td className="py-1.5 text-right text-orange-500">{fmt(e.interestAmount ?? e.interest)} FCFA</td>
                                  <td className="py-1.5 text-right text-gray-500">{fmt(e.remainingBalance)} FCFA</td>
                                  <td className="py-1.5 text-center">{e.paid ? <span className="text-green-600">✓ Payé</span> : <span className="text-amber-500">Attente</span>}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {modal && (
        <Modal isOpen onClose={() => { setModal(null); setErreur(''); }}
          title="Enregistrer un Paiement"
          subtitle={`${modal.clientFirstName} ${modal.clientLastName} — ${modal.loanNumber ?? modal.id?.slice(0, 10)}`}>
          <div className="space-y-4 min-w-[460px]">
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="bg-blue-50 rounded-xl p-3">
                <p className="text-xs text-blue-500">Mensualité</p>
                <p className="text-lg font-bold text-blue-800">{fmt(modal.monthlyPayment)}</p>
                <p className="text-xs text-blue-400">FCFA</p>
              </div>
              <div className="bg-gray-50 rounded-xl p-3">
                <p className="text-xs text-gray-500">Solde restant</p>
                <p className="text-lg font-bold text-gray-800">{fmt(modal.remainingBalance)}</p>
                <p className="text-xs text-gray-400">FCFA</p>
              </div>
              <div className="bg-amber-50 rounded-xl p-3">
                <p className="text-xs text-amber-500">Prochain paiement</p>
                <p className="text-sm font-bold text-amber-800">
                  {modal.nextPaymentDate ? new Date(modal.nextPaymentDate).toLocaleDateString('fr-FR') : '—'}
                </p>
              </div>
            </div>
            {amortModal && (() => {
              const next = amortModal.find(e => !e.paid);
              if (!next) return null;
              return (
                <div className="bg-indigo-50 border border-indigo-200 rounded-xl p-3 text-sm">
                  <p className="text-indigo-700 font-medium mb-1">Prochaine échéance — N°{next.installmentNumber}</p>
                  <div className="flex gap-5 text-xs text-indigo-600">
                    <span>Dû : <strong>{fmt(next.dueAmount)} FCFA</strong></span>
                    <span>Capital : {fmt(next.principalAmount ?? next.principal)} FCFA</span>
                    <span>Intérêts : {fmt(next.interestAmount ?? next.interest)} FCFA</span>
                  </div>
                </div>
              );
            })()}
            <form onSubmit={handleEnregistrer} className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="form-label">Montant (FCFA) *</label>
                  <input type="number" value={montant} onChange={e => setMontant(e.target.value)}
                    required min="0.01" step="any" className="form-input font-semibold" />
                </div>
                <div>
                  <label className="form-label">Mode de Paiement *</label>
                  <select value={modePaiement} onChange={e => setModePaiement(e.target.value)} className="form-input">
                    {PAYMENT_METHODS.map(m => <option key={m.value} value={m.value}>{m.label}</option>)}
                  </select>
                </div>
              </div>

              {modePaiement === 'MOBILE_MONEY' && (
                <div className="animate-in fade-in slide-in-from-top-1 duration-200">
                  <label className="form-label text-blue-700">N° Mobile Money du Client *</label>
                  <input
                    type="text"
                    value={numeroPaiement}
                    onChange={e => setNumeroPaiement(e.target.value)}
                    placeholder="Ex: 06xxx..."
                    required
                    className="form-input border-blue-200 bg-blue-50"
                  />
                </div>
              )}
              
              {modePaiement === 'BANK_TRANSFER' && (
                <div className="animate-in fade-in slide-in-from-top-1 duration-200">
                  <label className="form-label text-blue-700">Compte Source du Client *</label>
                  <select
                    value={compteSourceId}
                    onChange={e => setCompteSourceId(e.target.value)}
                    required
                    className="form-input border-blue-200 bg-blue-50"
                  >
                    <option value="">Sélectionner un compte</option>
                    {clientComptes.map(c => (
                      <option key={c.id} value={c.id}>{c.numeroCompte} — {c.typeCompte} ({fmt(c.solde)} FCFA)</option>
                    ))}
                  </select>
                </div>
              )}
              
              <div>
                <label className="form-label">N° de Reçu <span className="text-gray-400 font-normal">(optionnel)</span></label>
                <input type="text" value={numRecu} onChange={e => setNumRecu(e.target.value)}
                  placeholder="Ex: REC-2025-001" className="form-input" />
              </div>
              {erreur && (
                <div className="flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-lg text-sm">
                  <AlertTriangle size={14} className="mt-0.5 shrink-0" /> <span>{erreur}</span>
                </div>
              )}
              <div className="flex justify-end gap-3 pt-1">
                <button type="button" onClick={() => { setModal(null); setErreur(''); }} className="btn-secondary">Annuler</button>
                <button type="submit" disabled={saving} className="bg-emerald-700 hover:bg-emerald-800 text-white px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-2 disabled:opacity-60">
                  <Banknote size={14} /> {saving ? 'Enregistrement…' : 'Confirmer le Paiement'}
                </button>
              </div>
            </form>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default DirecteurRemboursementsPage;
