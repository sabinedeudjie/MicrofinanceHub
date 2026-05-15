import React, { useState, useEffect, useCallback } from 'react';
import { useLocation } from 'react-router-dom';
import { CreditCard, Plus, Clock, FileText, CheckCircle, Banknote, AlertTriangle } from 'lucide-react';
import Modal from '../../components/common/Modal';
import { getCurrentUser } from '../../utils/auth';
import { getClientLoans, applyForLoan, getAmortizationSchedule, getClientApplications } from '../../api/loansApi';
import { getComptesActifsByClient } from '../../api/comptesApi';
import { getMyClientProfile } from '../../api/clientsApi';
import { clientMakePayment } from '../../api/repaymentApi';

const fmt = (n) => n != null ? Number(n).toLocaleString('fr-FR') : '—';

const TYPES = ['Agriculture', 'Commerce', 'Artisanat', 'Éducation', 'Santé', 'Autre'];

const STATUS_CONFIG = {
  ACTIVE:    { label: 'Actif',       color: 'bg-green-100 text-green-700' },
  DISBURSED: { label: 'Décaissé',    color: 'bg-blue-100 text-blue-700' },
  PENDING:   { label: 'En attente',  color: 'bg-yellow-100 text-yellow-700' },
  APPROVED:  { label: 'Approuvé',    color: 'bg-indigo-100 text-indigo-700' },
  COMPLETED: { label: 'Terminé',     color: 'bg-gray-100 text-gray-600' },
  REJECTED:  { label: 'Rejeté',      color: 'bg-red-100 text-red-600' },
  DEFAULTED: { label: 'En défaut',   color: 'bg-red-200 text-red-700' },
};

const PretsClientPage = () => {
  const user     = getCurrentUser();
  const location = useLocation();

  const [prets,        setPrets]        = useState([]);
  const [demandes,     setDemandes]     = useState([]);
  const [loading,      setLoading]      = useState(true);
  const [modalPret,    setModalPret]    = useState(false);
  const [modalAmort,   setModalAmort]   = useState(null);
  const [amortData,    setAmortData]    = useState(null);
  const [modalPayer,   setModalPayer]   = useState(null);  // prêt sélectionné pour paiement
  const [amortPayer,   setAmortPayer]   = useState(null);
  const [payMontant,   setPayMontant]   = useState('');
  const [payMethod,    setPayMethod]    = useState('BANK_TRANSFER');
  const [payErreur,    setPayErreur]    = useState('');
  const [paying,       setPaying]       = useState(false);
  const [paySuccess,   setPaySuccess]   = useState(null);
  const [payNumero,    setPayNumero]    = useState('');
  const [saving,       setSaving]       = useState(false);
  const [erreur,       setErreur]       = useState('');
  const [comptes,      setComptes]      = useState([]);
  const [selectedCpt,  setSelectedCpt]  = useState('');
  const [successMsg,   setSuccessMsg]   = useState('');

  const [pretForm, setPretForm] = useState({
    requestedAmount: '', termMonths: '', loanType: '', purpose: '', accountNumber: '',
  });

  const clientId = user?.clientId ?? user?.id;

  const load = useCallback(async () => {
    if (!clientId) { setLoading(false); return; }
    setLoading(true);
    try {
      const [loansRes, appsRes] = await Promise.allSettled([
        getClientLoans(clientId),
        getClientApplications(clientId),
      ]);
      setPrets(loansRes.status === 'fulfilled' ? (loansRes.value.data ?? []) : []);
      const allApps = appsRes.status === 'fulfilled' ? (appsRes.value.data ?? []) : [];
      // Seules les demandes non encore converties en prêt (PENDING ou REJECTED)
      setDemandes(allApps.filter(a => a.status === 'PENDING' || a.status === 'REJECTED'));
    } catch { setPrets([]); setDemandes([]); }
    finally { setLoading(false); }
  }, [clientId]);

  useEffect(() => { load(); }, [load]);

  // Auto-ouvrir la modal si on arrive depuis le simulateur
  useEffect(() => {
    if (location.state?.openModal) {
      const { requestedAmount, termMonths, loanType } = location.state;
      setPretForm(f => ({
        ...f,
        requestedAmount: requestedAmount ?? '',
        termMonths:      termMonths      ?? '',
        loanType:        loanType        ?? '',
      }));
      setModalPret(true);
    }
  }, [location.state]);

  // Charger les comptes actifs du client quand la modal s'ouvre
  useEffect(() => {
    if (!modalPret) return;
    const fetchComptes = async () => {
      let cid = clientId;
      if (!cid) {
        try {
          const profileRes = await getMyClientProfile();
          cid = profileRes.data?.id;
          if (cid) {
            const stored = JSON.parse(localStorage.getItem('mfh_user') || '{}');
            localStorage.setItem('mfh_user', JSON.stringify({ ...stored, clientId: cid }));
          }
        } catch { return; }
      }
      if (!cid) return;
      try {
        const res = await getComptesActifsByClient(cid);
        const data = res.data?.data?.content ?? res.data?.data ?? res.data ?? [];
        setComptes(Array.isArray(data) ? data : []);
      } catch { setComptes([]); }
    };
    fetchComptes();
  }, [modalPret, clientId]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErreur('');
    setSaving(true);
    try {
      await applyForLoan({
        clientId,
        accountNumber:   pretForm.accountNumber,
        requestedAmount: parseFloat(pretForm.requestedAmount),
        termMonths:      parseInt(pretForm.termMonths),
        purpose:         pretForm.purpose ? `[${pretForm.loanType}] ${pretForm.purpose}` : pretForm.loanType,
      });
      await load();
      setModalPret(false);
      setPretForm({ requestedAmount: '', termMonths: '', loanType: '', purpose: '', accountNumber: '' });
      setSuccessMsg('Votre demande a bien été soumise et est en cours de traitement. Vous serez notifié par email dès qu\'une décision sera prise.');
      setTimeout(() => setSuccessMsg(''), 8000);
    } catch (err) {
      setErreur(err.response?.data?.message ?? 'Erreur lors de la soumission');
    } finally {
      setSaving(false);
    }
  };

  const openAmortissement = async (pret) => {
    setModalAmort(pret);
    setAmortData(null);
    try {
      const res = await getAmortizationSchedule(pret.id);
      setAmortData(res.data);
    } catch { setAmortData([]); }
  };

  const openPayer = async (pret) => {
    setModalPayer(pret);
    setPayErreur('');
    setPayMethod('BANK_TRANSFER');
    setSelectedCpt('');
    setAmortPayer(null);
    
    // Charger les comptes du client
    try {
      const cid = user?.clientId ?? user?.id;
      const res = await getComptesActifsByClient(cid);
      const data = res.data?.data?.content ?? res.data?.data ?? res.data ?? [];
      const list = Array.isArray(data) ? data : [];
      setComptes(list);
      if (list.length > 0) setSelectedCpt(list[0].id);
    } catch (err) {
      console.error("Erreur chargement comptes", err);
    }
    // Charger le profil pour avoir le numéro de téléphone par défaut
    try {
      const profileRes = await getMyClientProfile();
      if (profileRes.data?.phoneNumber) {
        setPayNumero(profileRes.data.phoneNumber);
      }
    } catch (err) {
      console.error("Erreur chargement profil", err);
    }
    try {
      const res = await getAmortizationSchedule(pret.id);
      const entries = res.data?.entries ?? res.data?.schedule ?? res.data ?? [];
      setAmortPayer(entries);
      const next = entries.find(e => !e.paid);
      setPayMontant(String(next?.dueAmount ?? pret.monthlyPayment ?? ''));
    } catch {
      setAmortPayer([]);
      setPayMontant(String(pret.monthlyPayment ?? ''));
    }
  };

  const handlePayer = async (e) => {
    e.preventDefault();
    if (!modalPayer) return;
    setPaying(true);
    setPayErreur('');
    try {
      const amount = parseFloat(payMontant);
      if (isNaN(amount) || amount <= 0) { setPayErreur('Montant invalide.'); setPaying(false); return; }
      
      if (payMethod === 'BANK_TRANSFER' && !selectedCpt) {
        setPayErreur('Veuillez sélectionner un compte pour le virement');
        setPaying(false);
        return;
      }

      await clientMakePayment({ 
        loanId: modalPayer.id, 
        amount, 
        paymentMethod: payMethod,
        numeroPaiement: payMethod === 'MOBILE_MONEY' ? payNumero : undefined,
        compteSourceId: payMethod === 'BANK_TRANSFER' ? selectedCpt : null
      });
      setPaySuccess(modalPayer.id);
      setModalPayer(null);
      await load();
      setTimeout(() => setPaySuccess(null), 5000);
    } catch (err) {
      const msg = err.response?.data?.message ?? err.response?.data ?? 'Erreur lors du paiement';
      setPayErreur(String(msg));
    } finally {
      setPaying(false);
    }
  };

  return (
    <div className="p-6 space-y-5">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Mes Prêts</h1>
          <p className="text-gray-500 text-sm">Suivez l'état de tous vos prêts</p>
        </div>
        <button onClick={() => setModalPret(true)} className="btn-primary flex items-center gap-2">
          <Plus size={16} /> Nouvelle Demande
        </button>
      </div>

      {successMsg && (
        <div className="flex items-start gap-3 bg-green-50 border border-green-200 text-green-800 rounded-xl px-4 py-3 text-sm">
          <CheckCircle size={18} className="text-green-600 mt-0.5 shrink-0" />
          <p>{successMsg}</p>
        </div>
      )}

      {loading ? (
        <div className="card flex items-center justify-center py-16 text-gray-400">Chargement...</div>
      ) : prets.length === 0 ? (
        <div className="card flex flex-col items-center justify-center py-16 text-gray-300">
          <CreditCard size={40} />
          <p className="text-sm mt-3">Aucun prêt enregistré</p>
          <button onClick={() => setModalPret(true)} className="btn-primary text-sm mt-4">
            Faire une demande
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {prets.map(p => {
            const paye    = Number(p.amount ?? 0) - Number(p.remainingBalance ?? 0);
            const pct     = p.amount ? Math.round((paye / Number(p.amount)) * 100) : 0;
            const cfg     = STATUS_CONFIG[p.status] ?? { label: p.status, color: 'bg-gray-100 text-gray-600' };
            return (
              <div key={p.id} className="card">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-blue-100 rounded-xl flex items-center justify-center">
                      <CreditCard size={18} className="text-blue-600" />
                    </div>
                    <div>
                      <p className="font-bold text-gray-800">{p.loanNumber ?? p.id?.slice(0, 12)}</p>
                      <p className="text-xs text-gray-400">{p.repaymentFrequency ?? 'Mensuel'}</p>
                    </div>
                  </div>
                  <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${cfg.color}`}>{cfg.label}</span>
                </div>

                <div className="grid grid-cols-4 gap-4 text-sm mb-4">
                  <div>
                    <p className="text-xs text-gray-400">Montant Total</p>
                    <p className="font-semibold text-gray-700">{fmt(p.amount)} FCFA</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">Mensualité</p>
                    <p className="font-semibold text-gray-700">{p.monthlyPayment ? fmt(p.monthlyPayment) + ' FCFA' : '—'}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">Déjà Payé</p>
                    <p className="font-semibold text-green-600">{fmt(paye)} FCFA</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">Restant</p>
                    <p className="font-semibold text-gray-700">{fmt(p.remainingBalance)} FCFA</p>
                  </div>
                </div>

                {p.status !== 'REJECTED' && p.status !== 'PENDING' && (
                  <div className="mb-3">
                    <div className="flex justify-between text-xs text-gray-400 mb-1">
                      <span>Progression</span>
                      <span>{pct}%</span>
                    </div>
                    <div className="h-2 bg-gray-200 rounded-full">
                      <div
                        className={`h-2 rounded-full transition-all ${pct >= 100 ? 'bg-green-500' : 'bg-blue-500'}`}
                        style={{ width: `${Math.min(pct, 100)}%` }}
                      />
                    </div>
                  </div>
                )}

                {(p.status === 'ACTIVE' || p.status === 'DISBURSED') && (
                  <div className="flex items-center justify-between pt-3 border-t border-gray-100">
                    <div>
                      <p className="text-xs text-gray-400">Prochaine Échéance</p>
                      <p className="font-medium text-gray-700 flex items-center gap-1">
                        <Clock size={13} className="text-blue-500" />
                        {p.nextPaymentDate
                          ? new Date(p.nextPaymentDate).toLocaleDateString('fr-FR')
                          : '—'}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      {paySuccess === p.id && (
                        <span className="flex items-center gap-1 text-green-600 text-xs font-medium">
                          <CheckCircle size={14} /> Paiement enregistré !
                        </span>
                      )}
                      <button onClick={() => openAmortissement(p)}
                        className="btn-secondary text-xs px-4 py-2">
                        Voir le Tableau
                      </button>
                      <button onClick={() => openPayer(p)}
                        className="btn-primary text-xs px-4 py-2 flex items-center gap-1.5">
                        <Banknote size={13} /> Rembourser
                      </button>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Section demandes en cours */}
      {demandes.length > 0 && (
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <FileText size={16} className="text-yellow-600" />
            <h2 className="font-semibold text-gray-700">Mes Demandes en Cours</h2>
          </div>
          <div className="space-y-3">
            {demandes.map(d => {
              const isPending  = d.status === 'PENDING';
              const isRejected = d.status === 'REJECTED';
              return (
                <div key={d.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-xl">
                  <div>
                    <p className="font-medium text-gray-800 text-sm">{d.applicationNumber ?? d.id?.slice(0, 12)}</p>
                    <p className="text-xs text-gray-400 mt-0.5">
                      {fmt(d.requestedAmount)} FCFA · {d.termMonths} mois
                      {d.applicationDate ? ` · ${new Date(d.applicationDate).toLocaleDateString('fr-FR')}` : ''}
                    </p>
                    {isRejected && d.rejectionReason && (
                      <p className="text-xs text-red-500 mt-1">Motif : {d.rejectionReason}</p>
                    )}
                  </div>
                  <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${
                    isPending  ? 'bg-yellow-100 text-yellow-700' :
                    isRejected ? 'bg-red-100 text-red-600'       :
                    'bg-gray-100 text-gray-600'
                  }`}>
                    {isPending ? 'En attente' : isRejected ? 'Rejetée' : d.status}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Modale Demande de Prêt */}
      <Modal isOpen={modalPret} onClose={() => { setModalPret(false); setErreur(''); }}
        title="Nouvelle Demande de Prêt" subtitle="Remplissez le formulaire pour soumettre votre demande">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="form-label">Compte associé *</label>
            <select value={pretForm.accountNumber}
              onChange={e => setPretForm(p => ({ ...p, accountNumber: e.target.value }))}
              required className="form-input">
              <option value="">Sélectionner un compte</option>
              {comptes.map(c => (
                <option key={c.id} value={c.numeroCompte}>
                  {c.numeroCompte} — {c.typeCompte} ({Number(c.solde).toLocaleString('fr-FR')} FCFA)
                </option>
              ))}
            </select>
            {comptes.length === 0 && (
              <p className="text-xs text-amber-500 mt-1">Vous devez avoir un compte actif pour faire une demande de prêt.</p>
            )}
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Montant (FCFA) *</label>
              <input type="number" value={pretForm.requestedAmount}
                onChange={e => setPretForm(p => ({ ...p, requestedAmount: e.target.value }))}
                placeholder="Ex: 500000" required min="50000" className="form-input" />
            </div>
            <div>
              <label className="form-label">Durée (mois) *</label>
              <input type="number" value={pretForm.termMonths}
                onChange={e => setPretForm(p => ({ ...p, termMonths: e.target.value }))}
                placeholder="Ex: 12" required min="3" max="60" className="form-input" />
            </div>
          </div>
          <div>
            <label className="form-label">Type de Prêt *</label>
            <select value={pretForm.loanType} onChange={e => setPretForm(p => ({ ...p, loanType: e.target.value }))}
              required className="form-input">
              <option value="">Sélectionner</option>
              {TYPES.map(t => <option key={t}>{t}</option>)}
            </select>
          </div>
          <div>
            <label className="form-label">Objet du Prêt *</label>
            <textarea rows={3} value={pretForm.purpose}
              onChange={e => setPretForm(p => ({ ...p, purpose: e.target.value }))}
              placeholder="Décrivez l'utilisation prévue du prêt..." required className="form-input resize-none" />
          </div>
          {erreur && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{erreur}</p>}
          <div className="flex justify-end gap-3">
            <button type="button" onClick={() => { setModalPret(false); setErreur(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={saving} className="btn-primary disabled:opacity-60">
              {saving ? 'Soumission...' : 'Soumettre la Demande'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modale Remboursement Client */}
      <Modal isOpen={!!modalPayer} onClose={() => { setModalPayer(null); setPayErreur(''); }}
        title="Effectuer un Remboursement"
        subtitle={modalPayer ? `${modalPayer.loanNumber ?? modalPayer.id?.slice(0, 12)}` : ''}>
        {modalPayer && (
          <div className="space-y-4 min-w-[420px]">

            {/* Résumé */}
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="bg-blue-50 rounded-xl p-3">
                <p className="text-xs text-blue-500 mb-0.5">Mensualité</p>
                <p className="text-lg font-bold text-blue-800">{Number(modalPayer.monthlyPayment).toLocaleString('fr-FR')}</p>
                <p className="text-xs text-blue-400">FCFA</p>
              </div>
              <div className="bg-gray-50 rounded-xl p-3">
                <p className="text-xs text-gray-500 mb-0.5">Solde restant</p>
                <p className="text-lg font-bold text-gray-800">{Number(modalPayer.remainingBalance).toLocaleString('fr-FR')}</p>
                <p className="text-xs text-gray-400">FCFA</p>
              </div>
              <div className="bg-green-50 rounded-xl p-3">
                <p className="text-xs text-green-500 mb-0.5">Déjà remboursé</p>
                <p className="text-lg font-bold text-green-700">
                  {Number(Math.max(0, (modalPayer.amount ?? 0) - (modalPayer.remainingBalance ?? 0))).toLocaleString('fr-FR')}
                </p>
                <p className="text-xs text-green-400">FCFA</p>
              </div>
            </div>

            {/* Prochaine échéance */}
            {amortPayer && (() => {
              const next = amortPayer.find(e => !e.paid);
              if (!next) return (
                <div className="bg-green-50 border border-green-200 text-green-700 px-3 py-2 rounded-xl text-sm flex items-center gap-2">
                  <CheckCircle size={15} /> Toutes les échéances sont payées
                </div>
              );
              return (
                <div className="bg-indigo-50 border border-indigo-200 rounded-xl p-3 text-sm">
                  <p className="text-indigo-700 font-medium mb-1">Échéance N°{next.installmentNumber}</p>
                  <div className="flex gap-4 text-xs text-indigo-600">
                    <span>À payer : <strong>{Number(next.dueAmount).toLocaleString('fr-FR')} FCFA</strong></span>
                    <span>Date : {next.dueDate ? new Date(next.dueDate).toLocaleDateString('fr-FR') : '—'}</span>
                  </div>
                </div>
              );
            })()}

            <form onSubmit={handlePayer} className="space-y-3">
              <div>
                <label className="form-label">Montant à rembourser (FCFA) *</label>
                <input type="number" value={payMontant} readOnly step="any"
                  className="form-input text-lg font-semibold bg-gray-50 cursor-not-allowed" />
                <p className="text-xs text-gray-400 mt-1">
                  Le montant doit couvrir exactement une ou plusieurs mensualités complètes.
                </p>
              </div>
              <div>
                <label className="form-label">Mode de Paiement *</label>
                <select value={payMethod} onChange={e => setPayMethod(e.target.value)} className="form-input">
                  <option value="BANK_TRANSFER">Virement bancaire (Compte MFH)</option>
                  <option value="MOBILE_MONEY">Mobile Money</option>
                </select>
              </div>

              {payMethod === 'MOBILE_MONEY' && (
                <div className="animate-in fade-in slide-in-from-top-1 duration-200">
                  <label className="form-label text-blue-700">Votre Numéro Mobile Money *</label>
                  <input
                    type="text"
                    value={payNumero}
                    onChange={e => setPayNumero(e.target.value)}
                    placeholder="Ex: 06xxx..."
                    required
                    className="form-input border-blue-200 bg-blue-50"
                  />
                </div>
              )}

              {payMethod === 'BANK_TRANSFER' && (
                <div className="animate-in fade-in slide-in-from-top-1 duration-200">
                  <label className="form-label text-blue-700 font-semibold">Sélectionner le compte source *</label>
                  <select value={selectedCpt} onChange={e => setSelectedCpt(e.target.value)} className="form-input border-blue-200 bg-blue-50">
                    {comptes.length === 0 ? (
                      <option value="">Aucun compte actif trouvé</option>
                    ) : (
                      comptes.map(c => (
                        <option key={c.id} value={c.id}>
                          {c.numeroCompte} - {c.typeCompte} ({Number(c.solde).toLocaleString('fr-FR')} FCFA)
                        </option>
                      ))
                    )}
                  </select>
                  {comptes.length === 0 && (
                    <p className="text-xs text-amber-600 mt-1 flex items-center gap-1">
                      <AlertTriangle size={12} /> Vous devez avoir un compte actif pour payer par virement.
                    </p>
                  )}
                </div>
              )}
              {payErreur && (
                <div className="flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-lg text-sm">
                  <AlertTriangle size={14} className="mt-0.5 shrink-0" />
                  <span>{payErreur}</span>
                </div>
              )}
              <div className="flex justify-end gap-3 pt-1">
                <button type="button" onClick={() => { setModalPayer(null); setPayErreur(''); }} className="btn-secondary">
                  Annuler
                </button>
                <button type="submit" disabled={paying} className="btn-primary flex items-center gap-2 disabled:opacity-60">
                  <Banknote size={14} /> {paying ? 'Traitement…' : 'Confirmer le Paiement'}
                </button>
              </div>
            </form>
          </div>
        )}
      </Modal>

      {/* Modale Tableau d'Amortissement */}
      <Modal isOpen={!!modalAmort} onClose={() => setModalAmort(null)}
        title="Tableau d'Amortissement"
        subtitle={modalAmort ? `${modalAmort.loanNumber} — ${fmt(modalAmort.amount)} FCFA` : ''}>
        {amortData === null ? (
          <p className="text-center text-gray-400 py-8">Chargement...</p>
        ) : !amortData || (Array.isArray(amortData) && amortData.length === 0) ? (
          <p className="text-center text-gray-400 py-8">Tableau non disponible</p>
        ) : (
          <div className="overflow-auto max-h-96">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-xs text-gray-400 border-b border-gray-100">
                  <th className="text-left pb-2">Mois</th>
                  <th className="text-right pb-2">Mensualité</th>
                  <th className="text-right pb-2">Capital</th>
                  <th className="text-right pb-2">Intérêts</th>
                  <th className="text-right pb-2">Solde Restant</th>
                </tr>
              </thead>
              <tbody>
                {(amortData.entries ?? amortData.schedule ?? amortData).map((e, i) => (
                  <tr key={i} className="border-b border-gray-50">
                    <td className="py-1.5 text-gray-600">{e.installmentNumber ?? e.month ?? i + 1}</td>
                    <td className="py-1.5 text-right font-medium">{fmt(e.dueAmount ?? e.monthlyPayment ?? e.payment)} FCFA</td>
                    <td className="py-1.5 text-right text-green-600">{fmt(e.principalAmount ?? e.principal ?? e.capitalAmount)} FCFA</td>
                    <td className="py-1.5 text-right text-orange-500">{fmt(e.interestAmount ?? e.interest)} FCFA</td>
                    <td className="py-1.5 text-right text-gray-500">{fmt(e.remainingBalance ?? e.balance)} FCFA</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default PretsClientPage;
