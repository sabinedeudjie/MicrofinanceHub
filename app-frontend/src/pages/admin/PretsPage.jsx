import React, { useState, useEffect, useCallback } from 'react';
import { FileText, Clock, CheckCircle, DollarSign, Search, Eye, ThumbsUp, ThumbsDown, Wallet, Banknote } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import StatCard from '../../components/common/StatCard';
import Modal from '../../components/common/Modal';
import { getLoanStats, getPendingApplications, getAllLoans, approveLoan, rejectLoan, disburseLoan, getAmortizationSchedule } from '../../api/loansApi';
import { agentRecordPayment } from '../../api/repaymentApi';
import { getComptesActifsByClient } from '../../api/comptesApi';

const fmt = (n) => n != null ? Number(n).toLocaleString('fr-FR') : '—';

const STATUS_LABELS = {
  PENDING:          'En attente',
  PENDING_APPROVAL: 'En attente',
  APPROVED:         'Approuvé',
  REJECTED:         'Rejeté',
  DISBURSED:        'Décaissé',
  ACTIVE:           'Actif',
  COMPLETED:        'Terminé',
  DEFAULTED:        'En défaut',
  WRITTEN_OFF:      'Passé en perte',
};

const STATUS_COLORS = {
  PENDING:          'bg-yellow-100 text-yellow-700',
  PENDING_APPROVAL: 'bg-yellow-100 text-yellow-700',
  APPROVED:         'bg-green-100 text-green-700',
  REJECTED:         'bg-red-100 text-red-600',
  DISBURSED:        'bg-blue-100 text-blue-700',
  ACTIVE:           'bg-emerald-100 text-emerald-700',
  COMPLETED:        'bg-gray-100 text-gray-600',
  DEFAULTED:        'bg-red-200 text-red-800',
  WRITTEN_OFF:      'bg-gray-200 text-gray-500',
};

const LOAN_STATUSES = [
  { value: '',                 label: 'Tous les statuts' },
  { value: 'PENDING_APPROVAL', label: 'En attente' },
  { value: 'APPROVED',         label: 'Approuvés' },
  { value: 'ACTIVE',           label: 'Actifs (décaissés)' },
  { value: 'COMPLETED',        label: 'Terminés' },
  { value: 'REJECTED',         label: 'Rejetés' },
  { value: 'DEFAULTED',        label: 'En défaut' },
  { value: 'WRITTEN_OFF',      label: 'Passés en perte' },
];

const TAUX_PAR_TYPE = {
  Agriculture: '5.0',
  Commerce:    '4.5',
  Artisanat:   '4.0',
  Éducation:   '6.0',
  Santé:       '3.5',
};

const detectRate = (purpose) => {
  if (!purpose) return '12.0';
  const match = purpose.match(/^\[(.*?)\]/);
  if (match && TAUX_PAR_TYPE[match[1]]) {
    return TAUX_PAR_TYPE[match[1]];
  }
  return '12.0';
};

const PretsPage = () => {
  const navigate = useNavigate();

  const [activeTab, setActiveTab]   = useState('demandes');
  const [loanStats, setLoanStats]   = useState(null);
  const [apps,      setApps]        = useState([]);
  const [loans,     setLoans]       = useState([]);
  const [loading,   setLoading]     = useState(true);
  const [search,    setSearch]      = useState('');
  const [loanStatusFilter, setLoanStatusFilter] = useState('');

  const [appsPage, setAppsPage]     = useState(0);
  const [appsTotalPages, setAppsTotalPages] = useState(1);
  const [loansPage, setLoansPage]   = useState(0);
  const [loansTotalPages, setLoansTotalPages] = useState(1);

  const [actioning, setActioning]   = useState(null);
  const [approvalModal, setApprovalModal] = useState(null);
  const [approvalForm, setApprovalForm]   = useState({ approvedAmount: '', approvedTermMonths: '', interestRate: '12.0' });
  const [actionError, setActionError]     = useState('');
  const [successMsg, setSuccessMsg]       = useState('');
  
  // États pour le remboursement (agent)
  const [modalRepayer, setModalRepayer]   = useState(null);
  const [repayForm, setRepayForm] = useState({
    amount: '', method: 'CASH', notes: '', receipt: '', compteSourceId: '', numeroPaiement: ''
  });
  const [repaying, setRepaying]           = useState(false);
  const [repayError, setRepayError]       = useState('');
  const [clientComptes, setClientComptes] = useState([]);

  const load = useCallback(async () => {
    setLoading(true);
    const [statsRes, appsRes, loansRes] = await Promise.allSettled([
      getLoanStats(),
      getPendingApplications(appsPage, 15),
      getAllLoans(loansPage, 20, loanStatusFilter || null),
    ]);
    if (statsRes.status === 'fulfilled') setLoanStats(statsRes.value.data);
    if (appsRes.status === 'fulfilled') {
      const data = appsRes.value.data;
      const content = data?.content ?? data ?? [];
      setApps(Array.isArray(content) ? content : []);
      setAppsTotalPages(data?.totalPages ?? 1);
    }
    if (loansRes.status === 'fulfilled') {
      const data = loansRes.value.data;
      const content = data?.content ?? data ?? [];
      setLoans(Array.isArray(content) ? content : []);
      setLoansTotalPages(data?.totalPages ?? 1);
    }
    setLoading(false);
  }, [appsPage, loansPage, loanStatusFilter]);

  useEffect(() => { load(); }, [load]);

  const handleApprove = (app) => {
    // Essayer de deviner le taux à partir du type dans le but [Type]
    let suggestedRate = '12.0';
    const typeMatch = app.purpose?.match(/\[(.*?)\]/);
    if (typeMatch) {
      const type = typeMatch[1];
      const rates = {
        'Agriculture': '5.0',
        'Commerce':    '4.5',
        'Artisanat':   '4.0',
        'Éducation':   '6.0',
        'Santé':       '3.5'
      };
      if (rates[type]) suggestedRate = rates[type];
    }

    setActionError('');
    setApprovalForm({
      approvedAmount:     String(app.requestedAmount ?? ''),
      approvedTermMonths: String(app.termMonths ?? ''),
      interestRate:       suggestedRate,
    });
    setApprovalModal(app);
  };

  const handleApproveSubmit = async (e) => {
    e.preventDefault();
    setActionError('');
    setActioning(approvalModal.id);
    try {
      await approveLoan(approvalModal.id, {
        approvedAmount:     parseFloat(approvalForm.approvedAmount),
        approvedTermMonths: parseInt(approvalForm.approvedTermMonths, 10),
        interestRate:       parseFloat(approvalForm.interestRate),
      });
      setApprovalModal(null);
      await load();
    } catch (err) {
      setActionError(err.response?.data?.message ?? "Erreur lors de l'approbation");
    } finally {
      setActioning(null);
    }
  };

  const handleReject = async (app) => {
    const reason = window.prompt('Raison du rejet (optionnel)');
    if (reason === null) return;
    setActioning(app.id);
    setActionError('');
    try {
      await rejectLoan(app.id, { rejectionReason: reason || 'Non spécifiée' });
      await load();
    } catch (err) {
      setActionError(err.response?.data?.message ?? 'Erreur lors du rejet');
    } finally {
      setActioning(null);
    }
  };

  const handleDisburse = async (loan) => {
    if (!window.confirm(`Décaisser le prêt ${loan.loanNumber ?? loan.id?.slice(0, 8)} de ${fmt(loan.amount)} FCFA pour ${loan.clientFirstName} ${loan.clientLastName} ?\n\nCette action est irréversible.`)) return;
    setActioning(loan.id);
    setActionError('');
    try {
      await disburseLoan(loan.id);
      await load();
    } catch (err) {
      setActionError(err.response?.data?.message ?? 'Erreur lors du décaissement');
    } finally {
      setActioning(null);
    }
  };

  const handleRepay = async (loan) => {
    setRepayError('');
    setRepayForm({
      amount: '',
      method: 'CASH',
      notes: '',
      receipt: '',
      compteSourceId: '',
      numeroPaiement: ''
    });
    setModalRepayer(loan);
    
    // Charger la prochaine échéance pour pré-remplir le montant
    try {
      const res = await getAmortizationSchedule(loan.id);
      const entries = res.data?.entries ?? res.data?.schedule ?? res.data ?? [];
      const next = entries.find(e => !e.paid);
      if (next) {
        setRepayForm(f => ({ ...f, amount: String(next.dueAmount) }));
      }
    } catch (err) {
      console.error("Erreur chargement amortissement", err);
    }
  };

  useEffect(() => {
    if (modalRepayer && repayForm.method === 'BANK_TRANSFER') {
      const loadComptes = async () => {
        try {
          const res = await getComptesActifsByClient(modalRepayer.clientId);
          const list = res.data?.content ?? res.data ?? [];
          setClientComptes(Array.isArray(list) ? list : []);
        } catch (err) {
          console.error("Erreur chargement comptes client", err);
        }
      };
      loadComptes();
    }
  }, [modalRepayer, repayForm.method]);

  const handleRepaySubmit = async (e) => {
    e.preventDefault();
    setRepayError('');
    setRepaying(true);
    try {
      await agentRecordPayment({
        loanId: modalRepayer.id,
        clientId: modalRepayer.clientId,
        amount: parseFloat(repayForm.amount),
        paymentMethod: repayForm.method,
        notes: repayForm.notes,
        receiptNumber: repayForm.receipt,
        numeroPaiement: repayForm.method === 'MOBILE_MONEY' ? repayForm.numeroPaiement : undefined,
        compteSourceId: repayForm.method === 'BANK_TRANSFER' ? repayForm.compteSourceId : null
      });
      setSuccessMsg(`Paiement de ${fmt(repayForm.amount)} FCFA enregistré pour ${modalRepayer.clientFirstName}`);
      setModalRepayer(null);
      await load();
      setTimeout(() => setSuccessMsg(''), 5000);
    } catch (err) {
      setRepayError(err.response?.data?.message ?? "Erreur lors de l'enregistrement du paiement");
    } finally {
      setRepaying(false);
    }
  };

  const filteredApps = apps.filter(a => {
    if (!search) return true;
    const q = search.toLowerCase();
    return (
      `${a.clientFirstName} ${a.clientLastName}`.toLowerCase().includes(q) ||
      a.clientEmail?.toLowerCase().includes(q) ||
      a.applicationNumber?.toLowerCase().includes(q)
    );
  });

  const filteredLoans = loans.filter(l => {
    if (!search) return true;
    const q = search.toLowerCase();
    return (
      `${l.clientFirstName} ${l.clientLastName}`.toLowerCase().includes(q) ||
      l.clientEmail?.toLowerCase().includes(q) ||
      l.loanNumber?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="p-6 space-y-6">

      <div>
        <h1 className="text-2xl font-bold text-gray-800">Gestion des Prêts</h1>
        <p className="text-gray-500 text-sm">Gérez le cycle de vie complet des prêts et demandes</p>
      </div>

      {successMsg && (
        <div className="flex items-start gap-3 bg-green-50 border border-green-200 text-green-800 rounded-xl px-4 py-3 text-sm animate-in fade-in slide-in-from-top-2 duration-300">
          <CheckCircle size={18} className="text-green-600 mt-0.5 shrink-0" />
          <p>{successMsg}</p>
        </div>
      )}

      {/* Statistiques */}
      <div className="grid grid-cols-4 gap-4">
        <StatCard title="Prêts en cours"   value={loading ? '...' : fmt(loanStats?.activeLoans)}           icon={FileText}    iconBg="bg-blue-100"   iconColor="text-blue-600" />
        <StatCard title="En Attente"       value={loading ? '...' : fmt(loanStats?.pendingApplications)}   icon={Clock}       iconBg="bg-yellow-100" iconColor="text-yellow-600" />
        <StatCard title="Approuvés"        value={loading ? '...' : fmt(loanStats?.approvedApplications)}  icon={CheckCircle} iconBg="bg-green-100"  iconColor="text-green-600" />
        <StatCard title="Encours (FCFA)"  value={loading ? '...' : fmt(loanStats?.outstandingAmount)}     icon={DollarSign}  iconBg="bg-purple-100" iconColor="text-purple-600" />
      </div>

      {/* Onglets */}
      <div className="border-b border-gray-200">
        <nav className="flex gap-6">
          {[
            { key: 'demandes', label: 'Demandes en attente', count: apps.length },
            { key: 'prets',    label: 'Historique & Portefeuille', count: null },
          ].map(tab => (
            <button key={tab.key}
              onClick={() => { setActiveTab(tab.key); setSearch(''); }}
              className={`pb-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.key
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}>
              {tab.label}
              {tab.count != null && (
                <span className={`ml-2 text-xs px-2 py-0.5 rounded-full ${
                  activeTab === tab.key ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-500'
                }`}>{tab.count}</span>
              )}
            </button>
          ))}
        </nav>
      </div>

      {/* ── Onglet Demandes ── */}
      {activeTab === 'demandes' && (
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="font-semibold text-gray-700">Demandes en Attente</h3>
              <p className="text-xs text-gray-400">{loading ? 'Chargement...' : `${apps.length} demande(s) à traiter`}</p>
            </div>
            <div className="relative">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input value={search} onChange={e => setSearch(e.target.value)}
                placeholder="Nom, email, référence..." className="form-input pl-9 w-60 text-sm" />
            </div>
          </div>

          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-400 border-b border-gray-100">
                <th className="text-left pb-3">Référence</th>
                <th className="text-left pb-3">Client</th>
                <th className="text-left pb-3">Montant demandé</th>
                <th className="text-left pb-3">Durée</th>
                <th className="text-left pb-3">Objet</th>
                <th className="text-left pb-3">Statut</th>
                <th className="text-left pb-3">Date</th>
                <th className="text-left pb-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={8} className="py-8 text-center text-gray-400">Chargement...</td></tr>
              ) : filteredApps.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-gray-300">
                    <FileText size={32} className="mx-auto mb-2" />
                    <p className="text-sm">Aucune demande en attente</p>
                  </td>
                </tr>
              ) : (
                filteredApps.map(a => (
                  <tr key={a.id} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 font-mono text-xs text-blue-700">{a.applicationNumber ?? a.id?.slice(0, 8)}</td>
                    <td className="py-3">
                      <p className="font-medium text-gray-800">{a.clientFirstName} {a.clientLastName}</p>
                      <p className="text-xs text-gray-400">{a.clientEmail}</p>
                    </td>
                    <td className="py-3 font-semibold">{fmt(a.requestedAmount)} FCFA</td>
                    <td className="py-3 text-gray-600">{a.termMonths} mois</td>
                    <td className="py-3 text-gray-500 text-xs max-w-[120px] truncate">{a.purpose ?? '—'}</td>
                    <td className="py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLORS[a.status] ?? 'bg-gray-100 text-gray-600'}`}>
                        {STATUS_LABELS[a.status] ?? a.status}
                      </span>
                    </td>
                    <td className="py-3 text-xs text-gray-400">
                      {a.applicationDate ? new Date(a.applicationDate).toLocaleDateString('fr-FR') : '—'}
                    </td>
                    <td className="py-3">
                      <div className="flex items-center gap-1">
                        {(a.status === 'PENDING_APPROVAL' || a.status === 'PENDING') && (
                          <>
                            <button onClick={() => handleApprove(a)} disabled={actioning === a.id}
                              className="p-1.5 hover:bg-green-50 text-gray-400 hover:text-green-600 rounded-lg" title="Approuver">
                              <ThumbsUp size={14} />
                            </button>
                            <button onClick={() => handleReject(a)} disabled={actioning === a.id}
                              className="p-1.5 hover:bg-red-50 text-gray-400 hover:text-red-500 rounded-lg" title="Rejeter">
                              <ThumbsDown size={14} />
                            </button>
                          </>
                        )}
                        <button onClick={() => navigate(`/admin/prets/${a.id}`)}
                          className="p-1.5 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded-lg" title="Voir le détail">
                          <Eye size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

          {appsTotalPages > 1 && (
            <div className="flex justify-center gap-2 mt-4">
              <button onClick={() => setAppsPage(p => Math.max(0, p - 1))} disabled={appsPage === 0} className="btn-secondary text-xs px-3 py-1.5">Précédent</button>
              <span className="text-xs text-gray-500 self-center">Page {appsPage + 1} / {appsTotalPages}</span>
              <button onClick={() => setAppsPage(p => Math.min(appsTotalPages - 1, p + 1))} disabled={appsPage >= appsTotalPages - 1} className="btn-secondary text-xs px-3 py-1.5">Suivant</button>
            </div>
          )}
        </div>
      )}

      {actionError && activeTab === 'prets' && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-2 rounded-xl text-sm">{actionError}</div>
      )}

      {/* ── Onglet Tous les prêts ── */}
      {activeTab === 'prets' && (
        <div className="card">
          <div className="flex items-center justify-between mb-4 gap-3 flex-wrap">
            <div>
              <h3 className="font-semibold text-gray-700 flex items-center gap-2">
                <Wallet size={16} className="text-blue-600" /> Portefeuille Prêts
              </h3>
              <p className="text-xs text-gray-400">{loading ? 'Chargement...' : `${filteredLoans.length} prêt(s)`}</p>
            </div>
            <div className="flex items-center gap-2">
              <select value={loanStatusFilter} onChange={e => { setLoanStatusFilter(e.target.value); setLoansPage(0); }}
                className="form-input text-sm w-44">
                {LOAN_STATUSES.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
              </select>
              <div className="relative">
                <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                <input value={search} onChange={e => setSearch(e.target.value)}
                  placeholder="Nom, email, numéro..." className="form-input pl-9 w-52 text-sm" />
              </div>
            </div>
          </div>

          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-400 border-b border-gray-100">
                <th className="text-left pb-3">N° Prêt</th>
                <th className="text-left pb-3">Client</th>
                <th className="text-left pb-3">Montant</th>
                <th className="text-left pb-3">Durée</th>
                <th className="text-left pb-3">Mensualité</th>
                <th className="text-left pb-3">Reste dû</th>
                <th className="text-left pb-3">Statut</th>
                <th className="text-left pb-3">Date</th>
                <th className="text-left pb-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={9} className="py-8 text-center text-gray-400">Chargement...</td></tr>
              ) : filteredLoans.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-gray-300">
                    <Wallet size={32} className="mx-auto mb-2" />
                    <p className="text-sm">Aucun prêt trouvé</p>
                  </td>
                </tr>
              ) : (
                filteredLoans.map(l => (
                  <tr key={l.id} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 font-mono text-xs text-blue-700">{l.loanNumber ?? l.id?.slice(0, 8)}</td>
                    <td className="py-3">
                      <p className="font-medium text-gray-800">{l.clientFirstName} {l.clientLastName}</p>
                      <p className="text-xs text-gray-400">{l.clientEmail}</p>
                    </td>
                    <td className="py-3 font-semibold">{fmt(l.amount)} FCFA</td>
                    <td className="py-3 text-gray-600">{l.termMonths} mois</td>
                    <td className="py-3 text-gray-600">{fmt(l.monthlyPayment)} FCFA</td>
                    <td className="py-3 text-gray-700 font-medium">{fmt(l.remainingBalance)} FCFA</td>
                    <td className="py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLORS[l.status] ?? 'bg-gray-100 text-gray-600'}`}>
                        {STATUS_LABELS[l.status] ?? l.status}
                      </span>
                    </td>
                    <td className="py-3 text-xs text-gray-400">
                      {l.approvalDate ? new Date(l.approvalDate).toLocaleDateString('fr-FR') : '—'}
                    </td>
                    <td className="py-3">
                      <div className="flex items-center gap-1">
                        {l.status === 'APPROVED' && (
                          <button
                            onClick={() => handleDisburse(l)}
                            disabled={actioning === l.id}
                            className="flex items-center gap-1 px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs rounded-lg font-medium disabled:opacity-60"
                            title="Décaisser ce prêt">
                            <Banknote size={13} />
                            {actioning === l.id ? '…' : 'Décaisser'}
                          </button>
                        )}
                        <button onClick={() => navigate(`/admin/prets/${l.id}?type=loan`)}
                          className="p-1.5 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded-lg" title="Voir le détail">
                          <Eye size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

          {loansTotalPages > 1 && (
            <div className="flex justify-center gap-2 mt-4">
              <button onClick={() => setLoansPage(p => Math.max(0, p - 1))} disabled={loansPage === 0} className="btn-secondary text-xs px-3 py-1.5">Précédent</button>
              <span className="text-xs text-gray-500 self-center">Page {loansPage + 1} / {loansTotalPages}</span>
              <button onClick={() => setLoansPage(p => Math.min(loansTotalPages - 1, p + 1))} disabled={loansPage >= loansTotalPages - 1} className="btn-secondary text-xs px-3 py-1.5">Suivant</button>
            </div>
          )}
        </div>
      )}

      {/* Modale approbation */}
      <Modal isOpen={!!approvalModal} onClose={() => { setApprovalModal(null); setActionError(''); }}
        title="Approuver la Demande"
        subtitle={approvalModal ? `${approvalModal.applicationNumber ?? approvalModal.id?.slice(0, 8)} — ${approvalModal.clientFirstName} ${approvalModal.clientLastName}` : ''}>
        <form onSubmit={handleApproveSubmit} className="space-y-4">
          <div>
            <label className="form-label">Montant approuvé (FCFA) *</label>
            <input type="number" min="1" step="any" required
              value={approvalForm.approvedAmount}
              onChange={e => setApprovalForm(p => ({ ...p, approvedAmount: e.target.value }))}
              className="form-input" />
          </div>
          <div>
            <label className="form-label">Durée approuvée (mois) *</label>
            <input type="number" min="1" step="any" required
              value={approvalForm.approvedTermMonths}
              onChange={e => setApprovalForm(p => ({ ...p, approvedTermMonths: e.target.value }))}
              className="form-input" />
          </div>
          <div>
            <label className="form-label">Taux d'intérêt (%) *</label>
            <input type="number" min="0" max="100" step="any" required
              value={approvalForm.interestRate}
              onChange={e => setApprovalForm(p => ({ ...p, interestRate: e.target.value }))}
              className="form-input" />
          </div>
          {actionError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{actionError}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setApprovalModal(null); setActionError(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={actioning === approvalModal?.id} className="btn-primary disabled:opacity-60">
              {actioning === approvalModal?.id ? 'Approbation…' : 'Approuver'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modale Remboursement (Agent) */}
      <Modal isOpen={!!modalRepayer} onClose={() => { setModalRepayer(null); setRepayError(''); }}
        title="Enregistrer un Remboursement"
        subtitle={modalRepayer ? `Prêt ${modalRepayer.loanNumber} — ${modalRepayer.clientFirstName} ${modalRepayer.clientLastName}` : ''}>
        <form onSubmit={handleRepaySubmit} className="space-y-4 min-w-[400px]">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Montant (FCFA) *</label>
              <input type="number" min="0.01" step="any" required
                value={repayForm.amount}
                onChange={e => setRepayForm(p => ({ ...p, amount: e.target.value }))}
                className="form-input" />
            </div>
            <div>
              <label className="form-label">Mode de Paiement *</label>
              <select value={repayForm.method}
                onChange={e => setRepayForm(p => ({ ...p, method: e.target.value }))}
                className="form-input">
                <option value="CASH">Espèces (Agence)</option>
                <option value="CHECK">Chèque (Agence)</option>
                <option value="BANK_TRANSFER">Virement Bancaire</option>
                <option value="MOBILE_MONEY">Mobile Money</option>
              </select>
            </div>
          </div>

          {repayForm.method === 'BANK_TRANSFER' && (
            <div className="animate-in fade-in slide-in-from-top-1 duration-200">
              <label className="form-label text-blue-700">Compte source du client *</label>
              <select required value={repayForm.compteSourceId}
                onChange={e => setRepayForm(p => ({ ...p, compteSourceId: e.target.value }))}
                className="form-input border-blue-200 bg-blue-50">
                <option value="">Sélectionner un compte</option>
                {clientComptes.map(c => (
                  <option key={c.id} value={c.id}>{c.numeroCompte} — {c.typeCompte} ({fmt(c.solde)} FCFA)</option>
                ))}
              </select>
            </div>
          )}

          {repayForm.method === 'MOBILE_MONEY' && (
            <div className="animate-in fade-in slide-in-from-top-1 duration-200">
              <label className="form-label text-blue-700">N° Mobile Money du Client *</label>
              <input
                type="text"
                value={repayForm.numeroPaiement}
                onChange={e => setRepayForm(p => ({ ...p, numeroPaiement: e.target.value }))}
                placeholder="Ex: 06xxx..."
                required
                className="form-input border-blue-200 bg-blue-50"
              />
            </div>
          )}


          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">N° de Reçu (optionnel)</label>
              <input type="text" value={repayForm.receipt}
                onChange={e => setRepayForm(p => ({ ...p, receipt: e.target.value }))}
                className="form-input" placeholder="Ex: R-12345" />
            </div>
            <div>
              <label className="form-label">Notes / Référence</label>
              <input type="text" value={repayForm.notes}
                onChange={e => setRepayForm(p => ({ ...p, notes: e.target.value }))}
                className="form-input" placeholder="Ex: Paiement anticipé" />
            </div>
          </div>

          {repayError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{repayError}</p>}
          
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalRepayer(null)} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={repaying} className="btn-primary flex items-center gap-2">
              <Banknote size={16} />
              {repaying ? 'Enregistrement…' : 'Valider le Remboursement'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default PretsPage;
