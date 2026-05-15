import React, { useState, useEffect, useCallback } from 'react';
import { CreditCard, Search, Wallet, ThumbsUp, ThumbsDown, Banknote } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import Modal from '../../components/common/Modal';
import { getPortfolioStatsForClients, getPendingApplicationsByClients, getLoansByClients, approveLoan, rejectLoan, disburseLoan } from '../../api/loansApi';
import { getMyAgencyClients } from '../../api/agencyApi';

const fmt = (v) => v != null ? new Intl.NumberFormat('fr-FR').format(v) : '—';
const fmtXAF = (v) => v != null ? new Intl.NumberFormat('fr-FR').format(v) + ' FCFA' : '—';

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

const DirecteurPretsPage = () => {
  const [activeTab, setActiveTab]   = useState('demandes');
  const [pending,   setPending]     = useState([]);
  const [loans,     setLoans]       = useState([]);
  const [portfolio, setPortfolio]   = useState(null);
  const [loading,   setLoading]     = useState(true);
  const [search,    setSearch]      = useState('');
  const [loanStatusFilter, setLoanStatusFilter] = useState('');
  const [loansPage, setLoansPage]   = useState(0);
  const [loansTotalPages, setLoansTotalPages] = useState(1);
  const [agencyClientIds, setAgencyClientIds] = useState(null);

  const [actioning, setActioning]   = useState(null);
  const [approvalModal, setApprovalModal] = useState(null);
  const [approvalForm, setApprovalForm]   = useState({ approvedAmount: '', approvedTermMonths: '', interestRate: '12.0' });
  const [actionError, setActionError]     = useState('');

  useEffect(() => {
    const loadAgencyClients = async () => {
      try {
        const res = await getMyAgencyClients();
        const ids = (res.data?.clients ?? []).map(c => c.clientId);
        setAgencyClientIds(ids);
      } catch {
        setAgencyClientIds([]);
      }
    };
    loadAgencyClients();
  }, []);

  const load = useCallback(async () => {
    if (agencyClientIds === null) return;
    setLoading(true);

    if (agencyClientIds.length === 0) {
      setPending([]);
      setLoans([]);
      setPortfolio(null);
      setLoading(false);
      return;
    }

    const [pendRes, porRes, loansRes] = await Promise.allSettled([
      getPendingApplicationsByClients(agencyClientIds, 0, 100),
      getPortfolioStatsForClients(agencyClientIds),
      getLoansByClients(agencyClientIds, loansPage, 20, loanStatusFilter || null),
    ]);
    if (pendRes.status  === 'fulfilled') setPending(pendRes.value.data?.content ?? []);
    if (porRes.status   === 'fulfilled') setPortfolio(porRes.value.data);
    if (loansRes.status === 'fulfilled') {
      const data = loansRes.value.data;
      setLoans(data?.content ?? []);
      setLoansTotalPages(data?.totalPages ?? 1);
    }
    setLoading(false);
  }, [agencyClientIds, loansPage, loanStatusFilter]);

  useEffect(() => { load(); }, [load]);

  const handleApprove = (app) => {
    setActionError('');
    setApprovalForm({
      approvedAmount:     String(app.requestedAmount ?? ''),
      approvedTermMonths: String(app.termMonths ?? ''),
      interestRate:       detectRate(app.purpose),
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
    try {
      await rejectLoan(app.id, { rejectionReason: reason || 'Non spécifiée' });
      await load();
    } catch (err) {
      alert(err.response?.data?.message ?? 'Erreur lors du rejet');
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

  const filteredPending = pending.filter(a => {
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
        <h1 className="text-2xl font-bold text-gray-800">Prêts de l'Agence</h1>
        <p className="text-gray-500 text-sm">Supervision des prêts et demandes</p>
      </div>

      {/* Statistiques portefeuille */}
      {portfolio && (
        <div className="grid grid-cols-3 gap-4">
          <StatCard title="Encours total" value={fmtXAF(portfolio.totalOutstanding)} icon={CreditCard} iconBg="bg-blue-100"   iconColor="text-blue-600" />
          <StatCard title="À risque 30j"  value={fmtXAF(portfolio.atRisk30Days)}     icon={CreditCard} iconBg="bg-orange-100" iconColor="text-orange-500" />
          <StatCard title="À risque 90j"  value={fmtXAF(portfolio.atRisk90Days)}     icon={CreditCard} iconBg="bg-red-100"    iconColor="text-red-500" />
        </div>
      )}

      {/* Onglets */}
      <div className="border-b border-gray-200">
        <nav className="flex gap-6">
          {[
            { key: 'demandes', label: 'Demandes en attente',   count: pending.length },
            { key: 'prets',    label: 'Historique & Portefeuille', count: null },
          ].map(tab => (
            <button key={tab.key}
              onClick={() => { setActiveTab(tab.key); setSearch(''); }}
              className={`pb-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.key
                  ? 'border-emerald-600 text-emerald-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}>
              {tab.label}
              {tab.count != null && (
                <span className={`ml-2 text-xs px-2 py-0.5 rounded-full ${
                  activeTab === tab.key ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'
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
            <h3 className="font-semibold text-gray-700">Demandes en Attente</h3>
            <div className="relative">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input value={search} onChange={e => setSearch(e.target.value)}
                placeholder="Nom, email..." className="form-input pl-9 w-52 text-sm" />
            </div>
          </div>
          {loading ? (
            <p className="text-center text-gray-400 py-8">Chargement...</p>
          ) : filteredPending.length === 0 ? (
            <div className="py-12 text-center text-gray-300">
              <CreditCard size={32} className="mx-auto mb-2" />
              <p className="text-sm">Aucune demande en attente</p>
            </div>
          ) : (
            <div className="table-container">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-xs text-gray-400 border-b border-gray-100">
                    <th className="text-left pb-3">Référence</th>
                    <th className="text-left pb-3">Client</th>
                    <th className="text-left pb-3">Montant</th>
                    <th className="text-left pb-3">Durée</th>
                    <th className="text-left pb-3">Statut</th>
                    <th className="text-left pb-3">Date</th>
                    <th className="text-left pb-3">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredPending.map(a => (
                    <tr key={a.id} className="border-b border-gray-50 hover:bg-gray-50">
                      <td className="py-3 text-xs font-mono text-gray-500">{a.applicationNumber ?? a.id?.slice(0, 8)}</td>
                      <td className="py-3">
                        <p className="font-medium text-gray-800">{a.clientFirstName} {a.clientLastName}</p>
                        <p className="text-xs text-gray-400">{a.clientEmail}</p>
                      </td>
                      <td className="py-3 font-medium">{fmt(a.requestedAmount)} FCFA</td>
                      <td className="py-3 text-gray-600">{a.termMonths} mois</td>
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
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ── Onglet Tous les prêts ── */}
      {activeTab === 'prets' && (
        <div className="card">
          <div className="flex items-center justify-between mb-4 gap-3 flex-wrap">
            <div>
              <h3 className="font-semibold text-gray-700 flex items-center gap-2">
                <Wallet size={16} className="text-emerald-600" /> Portefeuille Prêts
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
          {loading ? (
            <p className="text-center text-gray-400 py-8">Chargement...</p>
          ) : filteredLoans.length === 0 ? (
            <div className="py-12 text-center text-gray-300">
              <Wallet size={32} className="mx-auto mb-2" />
              <p className="text-sm">Aucun prêt trouvé</p>
            </div>
          ) : (
            <div className="table-container">
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
                    <th className="text-left pb-3">Date approbation</th>
                    <th className="text-left pb-3">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredLoans.map(l => (
                    <tr key={l.id} className="border-b border-gray-50 hover:bg-gray-50">
                      <td className="py-3 font-mono text-xs text-blue-700">{l.loanNumber ?? l.id?.slice(0, 8)}</td>
                      <td className="py-3">
                        <p className="font-medium text-gray-800">{l.clientFirstName} {l.clientLastName}</p>
                        <p className="text-xs text-gray-400">{l.clientEmail}</p>
                      </td>
                      <td className="py-3 font-semibold">{fmt(l.amount)} FCFA</td>
                      <td className="py-3 text-gray-600">{l.termMonths} mois</td>
                      <td className="py-3 text-gray-600">{fmt(l.monthlyPayment)} FCFA</td>
                      <td className="py-3 font-medium text-gray-700">{fmt(l.remainingBalance)} FCFA</td>
                      <td className="py-3">
                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLORS[l.status] ?? 'bg-gray-100 text-gray-600'}`}>
                          {STATUS_LABELS[l.status] ?? l.status}
                        </span>
                      </td>
                      <td className="py-3 text-xs text-gray-400">
                        {l.approvalDate ? new Date(l.approvalDate).toLocaleDateString('fr-FR') : '—'}
                      </td>
                      <td className="py-3">
                        {l.status === 'APPROVED' && (
                          <button
                            onClick={() => handleDisburse(l)}
                            disabled={actioning === l.id}
                            className="flex items-center gap-1 px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs rounded-lg font-medium disabled:opacity-60">
                            <Banknote size={13} />
                            {actioning === l.id ? '…' : 'Décaisser'}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
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
            <input type="number" min="1" step="any" required value={approvalForm.approvedAmount}
              onChange={e => setApprovalForm(p => ({ ...p, approvedAmount: e.target.value }))} className="form-input" />
          </div>
          <div>
            <label className="form-label">Durée approuvée (mois) *</label>
            <input type="number" min="1" step="any" required value={approvalForm.approvedTermMonths}
              onChange={e => setApprovalForm(p => ({ ...p, approvedTermMonths: e.target.value }))} className="form-input" />
          </div>
          <div>
            <label className="form-label">Taux d'intérêt (%) *</label>
            <input type="number" min="0" max="100" step="any" required value={approvalForm.interestRate}
              onChange={e => setApprovalForm(p => ({ ...p, interestRate: e.target.value }))} className="form-input" />
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
    </div>
  );
};

export default DirecteurPretsPage;
