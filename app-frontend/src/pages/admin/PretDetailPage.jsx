import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, CreditCard, FileText } from 'lucide-react';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import { getLoanById, getApplication, approveLoan, rejectLoan } from '../../api/loansApi';

const fmt = (n) => n != null ? Number(n).toLocaleString('fr-FR') : '—';
const fmtDate = (d) => d ? new Date(d).toLocaleDateString('fr-FR') : '—';

const STATUS_LABELS = {
  PENDING: 'En attente', APPROVED: 'Approuvé', REJECTED: 'Rejeté',
  DISBURSED: 'Décaissé', ACTIVE: 'Actif', COMPLETED: 'Terminé', DEFAULTED: 'En défaut',
};

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

const PretDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams();

  const [loan,          setLoan]          = useState(null);
  const [application,   setApplication]   = useState(null);
  const [loading,       setLoading]       = useState(true);
  const [loadError,     setLoadError]     = useState('');
  const [actionError,   setActionError]   = useState('');
  const [actioning,     setActioning]     = useState(false);
  const [approvalModal, setApprovalModal] = useState(false);
  const [approvalForm,  setApprovalForm]  = useState({ approvedAmount: '', approvedTermMonths: '', interestRate: '12.0' });

  useEffect(() => {
    const load = async () => {
      try {
        const res = await getLoanById(id);
        setLoan(res.data);
      } catch {
        // pas un prêt — essayer comme demande
        try {
          const appRes = await getApplication(id);
          setApplication(appRes.data);
        } catch {
          setLoadError('Dossier introuvable ou erreur de chargement.');
        }
      }
      setLoading(false);
    };
    load();
  }, [id]);

  const openApproveModal = () => {
    setApprovalForm({
      approvedAmount:    String(application?.requestedAmount ?? ''),
      approvedTermMonths: String(application?.termMonths ?? ''),
      interestRate:      detectRate(application?.purpose),
    });
    setActionError('');
    setApprovalModal(true);
  };

  const handleApprove = async (e) => {
    e.preventDefault();
    if (!application) return;
    setActioning(true);
    setActionError('');
    try {
      await approveLoan(application.id, {
        approvedAmount:     parseFloat(approvalForm.approvedAmount),
        approvedTermMonths: parseInt(approvalForm.approvedTermMonths, 10),
        interestRate:       parseFloat(approvalForm.interestRate),
      });
      navigate('/admin/prets');
    } catch (err) {
      setActionError(err.response?.data?.message ?? 'Erreur lors de l\'approbation');
    } finally {
      setActioning(false);
    }
  };

  const handleReject = async () => {
    if (!application) return;
    const reason = window.prompt('Raison du rejet (optionnel)');
    if (reason === null) return;
    setActioning(true);
    setActionError('');
    try {
      await rejectLoan(application.id, { rejectionReason: reason || 'Non spécifiée' });
      navigate('/admin/prets');
    } catch (err) {
      setActionError(err.response?.data?.message ?? 'Erreur lors du rejet');
    } finally {
      setActioning(false);
    }
  };

  if (loading) {
    return (
      <div className="p-6">
        <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-gray-500 hover:text-gray-700 text-sm font-medium mb-4">
          <ArrowLeft size={16} /> Retour
        </button>
        <p className="text-gray-400 text-center py-20">Chargement...</p>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="p-6">
        <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-gray-500 hover:text-gray-700 text-sm font-medium mb-4">
          <ArrowLeft size={16} /> Retour
        </button>
        <p className="text-red-500 text-center py-20">{loadError}</p>
      </div>
    );
  }

  // Vue demande (avant approbation)
  if (application && !loan) {
    const app = application;
    return (
      <div className="p-6 space-y-5">
        <button onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-gray-500 hover:text-gray-700 text-sm font-medium">
          <ArrowLeft size={16} /> Retour à la liste
        </button>

        <div className="card">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-yellow-100 rounded-xl flex items-center justify-center">
                <FileText size={22} className="text-yellow-600" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-gray-800">{app.applicationNumber ?? app.id}</h1>
                <p className="text-gray-400 text-sm">{app.clientFirstName} {app.clientLastName} — {app.clientEmail}</p>
              </div>
            </div>
            <span className={`text-sm px-3 py-1 rounded-full font-medium ${
              app.status === 'PENDING'  ? 'bg-yellow-100 text-yellow-700' :
              app.status === 'REJECTED' ? 'bg-red-100 text-red-600' :
              'bg-gray-100 text-gray-600'
            }`}>
              {STATUS_LABELS[app.status] ?? app.status}
            </span>
          </div>

          <div className="grid grid-cols-3 gap-4 mt-5 pt-5 border-t border-gray-100">
            {[
              ['Montant demandé', `${fmt(app.requestedAmount)} FCFA`],
              ['Durée',          `${app.termMonths ?? '—'} mois`],
              ['Date demande',   app.applicationDate ? new Date(app.applicationDate).toLocaleDateString('fr-FR') : '—'],
            ].map(([label, value]) => (
              <div key={label} className="text-center">
                <p className="text-xs text-gray-400">{label}</p>
                <p className="font-bold text-gray-800 mt-0.5">{value}</p>
              </div>
            ))}
          </div>

          {app.purpose && (
            <div className="mt-4 bg-gray-50 rounded-xl p-3 text-sm text-gray-600">
              <span className="text-xs text-gray-400 mr-2">Objet :</span>{app.purpose}
            </div>
          )}

          {app.rejectionReason && (
            <div className="mt-3 bg-red-50 rounded-xl p-3 text-sm text-red-700">
              <span className="text-xs font-semibold">Motif de rejet :</span> {app.rejectionReason}
            </div>
          )}

          {app.status === 'PENDING' && (
            <div className="flex items-center gap-3 mt-5 pt-4 border-t border-gray-100">
              <button onClick={openApproveModal} disabled={actioning}
                className="btn-primary flex items-center gap-2 disabled:opacity-60">
                Approuver la demande
              </button>
              <button onClick={handleReject} disabled={actioning}
                className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg text-sm font-medium disabled:opacity-60">
                {actioning ? 'Traitement...' : 'Rejeter'}
              </button>
            </div>
          )}
          {actionError && <p className="text-red-500 text-sm mt-3">{actionError}</p>}
        </div>

        <Modal
          isOpen={approvalModal}
          onClose={() => { setApprovalModal(false); setActionError(''); }}
          title="Approuver la Demande"
          subtitle={`${app.applicationNumber ?? app.id} — ${app.clientFirstName} ${app.clientLastName}`}
        >
          <form onSubmit={handleApprove} className="space-y-4">
            <div>
              <label className="form-label">Montant approuvé (FCFA) *</label>
              <input type="number" min="1" step="any" required
                value={approvalForm.approvedAmount}
                onChange={e => setApprovalForm({ ...approvalForm, approvedAmount: e.target.value })}
                className="form-input" />
            </div>
            <div>
              <label className="form-label">Durée approuvée (mois) *</label>
              <input type="number" min="1" step="any" required
                value={approvalForm.approvedTermMonths}
                onChange={e => setApprovalForm({ ...approvalForm, approvedTermMonths: e.target.value })}
                className="form-input" />
            </div>
            <div>
              <label className="form-label">Taux d'intérêt (%) *</label>
              <input type="number" min="0" max="100" step="any" required
                value={approvalForm.interestRate}
                onChange={e => setApprovalForm({ ...approvalForm, interestRate: e.target.value })}
                className="form-input" />
            </div>
            {actionError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{actionError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => { setApprovalModal(false); setActionError(''); }} className="btn-secondary">Annuler</button>
              <button type="submit" disabled={actioning} className="btn-primary disabled:opacity-60">
                {actioning ? 'Approbation…' : 'Approuver'}
              </button>
            </div>
          </form>
        </Modal>
      </div>
    );
  }

  if (!loan) return null;

  const schedule      = loan.amortizationSchedule ?? [];
  const paidCount     = schedule.filter(e => e.paid).length;
  const totalCount    = schedule.length || loan.termMonths || 1;
  const progression   = Math.round((paidCount / totalCount) * 100);
  const clientName    = `${loan.clientFirstName ?? ''} ${loan.clientLastName ?? ''}`.trim() || `Client ${loan.clientId}`;

  return (
    <div className="p-6 space-y-5">

      <button onClick={() => navigate(-1)}
        className="flex items-center gap-2 text-gray-500 hover:text-gray-700 text-sm font-medium">
        <ArrowLeft size={16} /> Retour à la liste
      </button>

      <div className="card">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center">
              <CreditCard size={22} className="text-blue-600" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-gray-800">{loan.loanNumber ?? loan.id}</h1>
              <p className="text-gray-400 text-sm">{clientName}</p>
            </div>
          </div>
          <Badge status={String(loan.status ?? '').toLowerCase()} />
        </div>

        <div className="grid grid-cols-5 gap-4 mt-5 pt-5 border-t border-gray-100">
          {[
            ['Montant',        `${fmt(loan.amount)} FCFA`],
            ['Taux',           loan.interestRate != null ? `${Number(loan.interestRate).toFixed(1)}%` : '—'],
            ['Durée',          `${loan.termMonths ?? '—'} mois`],
            ['Mensualité',     `${fmt(loan.monthlyPayment)} FCFA`],
            ['Décaissement',   fmtDate(loan.disbursementDate)],
          ].map(([label, value]) => (
            <div key={label} className="text-center">
              <p className="text-xs text-gray-400">{label}</p>
              <p className="font-bold text-gray-800 mt-0.5">{value}</p>
            </div>
          ))}
        </div>

        {loan.maturityDate && (
          <div className="mt-4 bg-gray-50 rounded-xl p-3 text-sm text-gray-600">
            <span className="text-xs text-gray-400 mr-2">Échéance finale :</span>
            {fmtDate(loan.maturityDate)}
            {loan.nextPaymentDate && (
              <span className="ml-4"><span className="text-xs text-gray-400">Prochain paiement :</span> {fmtDate(loan.nextPaymentDate)}</span>
            )}
          </div>
        )}

        <div className="mt-4">
          <div className="flex justify-between text-xs text-gray-400 mb-1">
            <span>Progression des remboursements</span>
            <span>{paidCount}/{totalCount} mensualités — {progression}%</span>
          </div>
          <div className="w-full h-2.5 bg-gray-200 rounded-full">
            <div className="h-2.5 bg-green-500 rounded-full transition-all" style={{ width: `${progression}%` }} />
          </div>
        </div>
      </div>

      {schedule.length > 0 && (
        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-4">Échéancier de Remboursement</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-xs text-gray-400 border-b border-gray-100">
                  <th className="text-left pb-3">N°</th>
                  <th className="text-left pb-3">Mensualité (FCFA)</th>
                  <th className="text-left pb-3">Capital (FCFA)</th>
                  <th className="text-left pb-3">Intérêts (FCFA)</th>
                  <th className="text-left pb-3">Solde Restant (FCFA)</th>
                  <th className="text-left pb-3">Échéance</th>
                  <th className="text-left pb-3">Statut</th>
                </tr>
              </thead>
              <tbody>
                {schedule.map(e => (
                  <tr key={e.installmentNumber}
                    className={`border-b border-gray-50 text-xs ${e.paid ? 'bg-green-50/50' : ''}`}>
                    <td className="py-2 font-medium text-gray-600">Mois {e.installmentNumber}</td>
                    <td className="py-2 font-semibold text-gray-800">{fmt(e.dueAmount)}</td>
                    <td className="py-2 text-gray-600">{fmt(e.principalAmount)}</td>
                    <td className="py-2 text-blue-500">{fmt(e.interestAmount)}</td>
                    <td className="py-2 text-gray-600">{fmt(e.remainingBalance)}</td>
                    <td className="py-2 text-gray-400">{fmtDate(e.dueDate)}</td>
                    <td className="py-2">
                      <Badge status={e.paid ? 'a_jour' : 'a_venir'} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default PretDetailPage;
