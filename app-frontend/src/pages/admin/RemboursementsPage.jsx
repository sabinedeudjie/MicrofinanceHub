import React, { useState, useEffect, useCallback } from 'react';
import { Calendar, CheckCircle, AlertTriangle, TrendingUp, Banknote } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import Modal from '../../components/common/Modal';
import { agentRecordPayment, getRepaymentStats } from '../../api/repaymentApi';
import { getAllLoans, getLoanStats, getAmortizationSchedule } from '../../api/loansApi';
import { getComptesActifsByClient } from '../../api/comptesApi';

const fmt = (n) => (n != null ? Number(n).toLocaleString('fr-FR') : '—');
const fmtPct = (n) => (n != null ? `${Number(n).toFixed(1)} %` : '—');

// Retourne une chaîne ISO sans timezone pour que Spring puisse parser en LocalDateTime
const toLocalISO = (dateStr) => dateStr; // dateStr est déjà au format "yyyy-MM-ddTHH:mm:ss"

const PAYMENT_METHODS = [
  { value: 'CASH',          label: 'Espèces' },
  { value: 'MOBILE_MONEY',  label: 'Mobile Money' },
  { value: 'BANK_TRANSFER', label: 'Virement bancaire' },
  { value: 'CHECK',         label: 'Chèque' },
];

const RemboursementsPage = () => {
  const today = new Date();
  const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);

  const [startDate, setStartDate] = useState(firstDay.toISOString().slice(0, 10));
  const [endDate,   setEndDate]   = useState(today.toISOString().slice(0, 10));

  const [repayStats, setRepayStats] = useState(null);
  const [loanStats,  setLoanStats]  = useState(null);
  const [activeLoans, setActiveLoans] = useState([]);
  const [pendingPayments, setPendingPayments] = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [loadingLoans, setLoadingLoans] = useState(true);
  const [loadingPending, setLoadingPending] = useState(false);

  // Modal paiement admin
  const [modal,        setModal]        = useState(null);
  const [amortModal,   setAmortModal]   = useState(null);
  const [montant,      setMontant]      = useState('');
  const [modePaiement, setModePaiement] = useState('CASH');
  const [numRecu,      setNumRecu]      = useState('');
  const [numeroPaiement, setNumeroPaiement] = useState('');
  const [compteSourceId, setCompteSourceId] = useState('');
  const [clientComptes,  setClientComptes]  = useState([]);
  const [saving,       setSaving]       = useState(false);
  const [validating,   setValidating]   = useState(null); // ID du paiement en cours de validation
  const [erreur,       setErreur]       = useState('');
  const [successId,    setSuccessId]    = useState(null);
  const [validSuccess, setValidSuccess] = useState(null);

  const loadStats = useCallback(async () => {
    setLoading(true);
    const sd = toLocalISO(startDate + 'T00:00:00');
    const ed = toLocalISO(endDate   + 'T23:59:59');
    const [rs, ls] = await Promise.allSettled([
      getRepaymentStats(sd, ed),
      getLoanStats(sd, ed),
    ]);
    if (rs.status === 'fulfilled') setRepayStats(rs.value.data);
    if (ls.status === 'fulfilled') setLoanStats(ls.value.data);
    setLoading(false);
  }, [startDate, endDate]);

  const loadLoans = useCallback(async () => {
    setLoadingLoans(true);
    try {
      const res = await getAllLoans(0, 200, 'ACTIVE');
      const data = res.data?.content ?? res.data ?? [];
      setActiveLoans(Array.isArray(data) ? data : []);
    } catch { setActiveLoans([]); }
    finally { setLoadingLoans(false); }
  }, []);

  const loadPending = useCallback(async () => {
    setLoadingPending(true);
    try {
      const { getPendingPayments } = await import('../../api/repaymentApi');
      const res = await getPendingPayments();
      setPendingPayments(res.data ?? []);
    } catch (err) {
      console.error("Erreur chargement paiements en attente", err);
    } finally {
      setLoadingPending(false);
    }
  }, []);

  useEffect(() => { loadStats(); }, [loadStats]);
  useEffect(() => { loadLoans(); }, [loadLoans]);
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
      await Promise.all([loadPending(), loadStats(), loadLoans()]);
    } catch (err) {
      alert("Erreur lors de la validation: " + (err.response?.data?.message ?? "Erreur technique"));
    } finally {
      setValidating(null);
    }
  };

  const openModal = async (loan) => {
    setModal(loan);
    setErreur('');
    setNumRecu('');
    setNumeroPaiement('');
    setCompteSourceId('');
    setModePaiement('CASH');
    setAmortModal(null);
    try {
      const res = await getAmortizationSchedule(loan.id);
      const entries = res.data?.entries ?? res.data?.schedule ?? res.data ?? [];
      setAmortModal(entries);
      const next = entries.find(e => !e.paid);
      setMontant(String(next?.dueAmount ?? loan.monthlyPayment ?? ''));
    } catch {
      setAmortModal([]);
      setMontant(String(loan.monthlyPayment ?? ''));
    }
  };

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
      await Promise.all([loadStats(), loadLoans()]);
      setTimeout(() => setSuccessId(null), 4000);
    } catch (err) {
      setErreur(String(err.response?.data?.message ?? err.response?.data ?? 'Erreur'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="p-6 space-y-6">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Gestion des Remboursements</h1>
          <p className="text-gray-500 text-sm">Suivez les encaissements et enregistrez des paiements</p>
        </div>
        {/* Filtre période */}
        <div className="flex items-center gap-2 text-sm">
          <span className="text-gray-400">Du</span>
          <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)} className="form-input text-sm w-36" />
          <span className="text-gray-400">au</span>
          <input type="date" value={endDate}   onChange={e => setEndDate(e.target.value)}   className="form-input text-sm w-36" />
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-4 gap-4">
        <StatCard title="Prêts en Cours"        value={loading ? '…' : fmt(loanStats?.activeLoans)}          icon={Calendar}      iconBg="bg-blue-100"   iconColor="text-blue-600" />
        <StatCard title="Total Encaissé (FCFA)"  value={loading ? '…' : fmt(repayStats?.totalRepayments)}    icon={CheckCircle}   iconBg="bg-green-100"  iconColor="text-green-600"
          subtitle={repayStats ? `${fmt(repayStats.totalTransactions)} transactions` : undefined} />
        <StatCard title="En Retard (FCFA)"       value={loading ? '…' : fmt(repayStats?.overdueAmount)}      icon={AlertTriangle} iconBg="bg-red-100"    iconColor="text-red-500"
          subtitle={repayStats ? `${fmt(repayStats.overdueCount)} dossiers` : undefined} />
        <StatCard title="Taux de Recouvrement"   value={loading ? '…' : fmtPct(repayStats?.repaymentRate)}  icon={TrendingUp}    iconBg="bg-purple-100" iconColor="text-purple-600" />
      </div>

      {/* Grille synthèse */}
      <div className="grid grid-cols-2 gap-4">
        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-1">Synthèse des Remboursements</h3>
          <p className="text-xs text-gray-400 mb-4">Période sélectionnée</p>
          {repayStats ? (
            <div className="space-y-3 text-sm">
              {[
                ['Total encaissé',        fmt(repayStats.totalRepayments) + ' FCFA'],
                ['Nombre de transactions', fmt(repayStats.totalTransactions)],
                ['Montant en retard',      fmt(repayStats.overdueAmount)   + ' FCFA'],
                ['Dossiers en retard',     fmt(repayStats.overdueCount)],
                ['Taux de recouvrement',   fmtPct(repayStats.repaymentRate)],
              ].map(([label, val]) => (
                <div key={label} className="flex justify-between border-b border-gray-50 pb-2">
                  <span className="text-gray-500">{label}</span>
                  <span className="font-semibold text-gray-800">{val}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex items-center justify-center h-32 text-gray-300 text-sm">
              {loading ? 'Chargement…' : 'Aucune donnée'}
            </div>
          )}
        </div>

        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-1">Performance des Prêts</h3>
          <p className="text-xs text-gray-400 mb-4">Global portefeuille</p>
          {loanStats ? (
            <div className="space-y-3 text-sm">
              {[
                ['Total décaissé',  fmt(loanStats.totalDisbursedAmount) + ' FCFA'],
                ['Total remboursé', fmt(loanStats.totalRepaidAmount)    + ' FCFA'],
                ['Encours restant', fmt(loanStats.outstandingAmount)    + ' FCFA'],
                ['Prêts terminés',  fmt(loanStats.completedLoans)],
                ['Prêts en défaut', fmt(loanStats.defaultedLoans)],
                ['Taux de défaut',  fmtPct(loanStats.defaultRate)],
              ].map(([label, val]) => (
                <div key={label} className="flex justify-between border-b border-gray-50 pb-2">
                  <span className="text-gray-500">{label}</span>
                  <span className="font-semibold text-gray-800">{val}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex items-center justify-center h-32 text-gray-300 text-sm">
              {loading ? 'Chargement…' : 'Aucune donnée'}
            </div>
          )}
        </div>
      </div>

      {/* Paiements en attente de validation (ADMIN ONLY) */}
      <div className="card border-amber-100 bg-amber-50/20">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2 text-amber-800">
            <AlertTriangle size={18} />
            <h3 className="font-semibold">Paiements en Attente de Validation</h3>
          </div>
          <p className="text-xs text-amber-600 bg-amber-100 px-2 py-0.5 rounded-full font-medium">
            {loadingPending ? 'Chargement…' : `${pendingPayments.length} à valider`}
          </p>
        </div>
        
        {loadingPending ? (
          <div className="py-6 text-center text-gray-400">Chargement des paiements…</div>
        ) : pendingPayments.length === 0 ? (
          <div className="py-6 text-center text-gray-400 italic text-sm">
            Aucun paiement en attente de validation pour le moment.
          </div>
        ) : (
          <div className="overflow-auto max-h-80">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-xs text-amber-700/60 border-b border-amber-100">
                  <th className="text-left pb-2">Client / Prêt</th>
                  <th className="text-left pb-2">Collecté par</th>
                  <th className="text-right pb-2">Montant</th>
                  <th className="text-left pb-2 px-3">Méthode</th>
                  <th className="text-left pb-2">Date</th>
                  <th className="pb-2"></th>
                </tr>
              </thead>
              <tbody>
                {pendingPayments.map(p => (
                  <tr key={p.id} className="border-b border-amber-50/50 hover:bg-white/50">
                    <td className="py-3">
                      <p className="font-medium text-gray-800">{p.clientName}</p>
                      <p className="text-[10px] font-mono text-gray-400">{p.paymentNumber}</p>
                    </td>
                    <td className="py-3 text-xs text-gray-600 italic">
                      {p.registeredBy || 'Agent'}
                    </td>
                    <td className="py-3 text-right font-bold text-gray-800">
                      {fmt(p.amount)} FCFA
                    </td>
                    <td className="py-3 px-3">
                      <span className="text-[10px] uppercase font-bold bg-white border border-amber-200 px-1.5 py-0.5 rounded text-amber-700">
                        {p.paymentMethod}
                      </span>
                    </td>
                    <td className="py-3 text-xs text-gray-400">
                      {p.paymentDate ? new Date(p.paymentDate).toLocaleDateString('fr-FR') : '—'}
                    </td>
                    <td className="py-3 text-right">
                      {validSuccess === p.id ? (
                        <span className="text-green-600 font-bold flex items-center gap-1 justify-end">
                          <CheckCircle size={14} /> Validé
                        </span>
                      ) : (
                        <button 
                          onClick={() => handleValidate(p.id)}
                          disabled={validating === p.id}
                          className="bg-amber-600 hover:bg-amber-700 text-white px-3 py-1.5 rounded-lg text-xs font-bold shadow-sm disabled:opacity-50 transition-colors"
                        >
                          {validating === p.id ? 'Vérification…' : 'Valider'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Prêts actifs — enregistrer un paiement */}
      <div className="card">
        <h3 className="font-semibold text-gray-700 mb-1">Prêts Actifs — Enregistrer un Paiement</h3>
        <p className="text-xs text-gray-400 mb-4">
          {loadingLoans ? 'Chargement…' : `${activeLoans.length} prêt(s) actif(s)`}
        </p>
        <table className="w-full text-sm">
          <thead>
            <tr className="text-xs text-gray-400 border-b border-gray-100">
              <th className="text-left pb-3">Client</th>
              <th className="text-left pb-3">Référence</th>
              <th className="text-right pb-3">Montant</th>
              <th className="text-right pb-3">Mensualité</th>
              <th className="text-right pb-3">Restant dû</th>
              <th className="text-left pb-3">Prochain paiement</th>
              <th className="pb-3"></th>
            </tr>
          </thead>
          <tbody>
            {loadingLoans ? (
              <tr><td colSpan={7} className="py-8 text-center text-gray-400">Chargement…</td></tr>
            ) : activeLoans.length === 0 ? (
              <tr><td colSpan={7} className="py-10 text-center text-gray-300">Aucun prêt actif</td></tr>
            ) : (
              activeLoans.map(loan => (
                <tr key={loan.id} className={`border-b border-gray-50 hover:bg-gray-50 ${successId === loan.id ? 'bg-green-50' : ''}`}>
                  <td className="py-3">
                    <p className="font-medium text-gray-800">{loan.clientFirstName} {loan.clientLastName}</p>
                    <p className="text-xs text-gray-400">{loan.clientEmail}</p>
                  </td>
                  <td className="py-3 font-mono text-xs text-blue-700">{loan.loanNumber ?? loan.id?.slice(0, 10)}</td>
                  <td className="py-3 text-right">{fmt(loan.amount)} FCFA</td>
                  <td className="py-3 text-right font-semibold">{fmt(loan.monthlyPayment)} FCFA</td>
                  <td className="py-3 text-right text-gray-600">{fmt(loan.remainingBalance)} FCFA</td>
                  <td className="py-3 text-xs text-gray-400">
                    {loan.nextPaymentDate ? new Date(loan.nextPaymentDate).toLocaleDateString('fr-FR') : '—'}
                  </td>
                  <td className="py-3 text-right">
                    {successId === loan.id ? (
                      <span className="text-green-600 text-xs font-medium flex items-center gap-1 justify-end">
                        <CheckCircle size={13} /> Enregistré
                      </span>
                    ) : (
                      <button onClick={() => openModal(loan)} className="btn-primary text-xs px-3 py-1.5 flex items-center gap-1.5 ml-auto">
                        <Banknote size={13} /> Paiement
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Modal paiement */}
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
                <button type="submit" disabled={saving} className="btn-primary flex items-center gap-2 disabled:opacity-60">
                  <Banknote size={14} /> {saving ? 'Enregistrement…' : 'Confirmer'}
                </button>
              </div>
            </form>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default RemboursementsPage;
