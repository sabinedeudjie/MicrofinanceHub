import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft, User, CreditCard, TrendingDown, TrendingUp,
  ArrowLeftRight, ChevronDown, ChevronUp, UserCheck, FileText, Eye, PlusCircle,
} from 'lucide-react';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import { getClientById } from '../../api/clientsApi';
import { getComptesByClient, ouvrirCompte } from '../../api/comptesApi';
import { getTransactions } from '../../api/transactionsApi';
import { getMyAgencyAgents } from '../../api/agencyApi';
import { getClientDocuments } from '../../api/documentsApi';

const TYPE_LABELS = {
  EPARGNE: 'Épargne', COURANT: 'Courant',
  MICRO_EPARGNE: 'Micro-Épargne', DEPOT_A_TERME: 'Dépôt à Terme', CREDIT: 'Crédit',
};

const STATUT_COMPTE = {
  EN_ATTENTE_VALIDATION: { cls: 'bg-yellow-100 text-yellow-700', label: 'En attente' },
  ACTIF:  { cls: 'bg-green-100 text-green-700',  label: 'Actif' },
  BLOQUE: { cls: 'bg-red-100 text-red-700',       label: 'Bloqué' },
  FERME:  { cls: 'bg-gray-100 text-gray-500',     label: 'Fermé' },
  REJETE: { cls: 'bg-red-100 text-red-700',       label: 'Rejeté' },
};

const StatutBadge = ({ s }) => {
  const cfg = STATUT_COMPTE[s] ?? { cls: 'bg-gray-100 text-gray-500', label: s };
  return <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${cfg.cls}`}>{cfg.label}</span>;
};

const txColor = (type) => {
  if (type === 'DEPOT') return 'text-green-600';
  if (type === 'RETRAIT') return 'text-red-500';
  return 'text-blue-600';
};
const txPrefix = (type) => (type === 'DEPOT' ? '+' : type === 'RETRAIT' ? '-' : '');
const TxIcon = ({ type }) => {
  if (type === 'DEPOT')    return <TrendingDown  size={13} className="text-green-500 shrink-0" />;
  if (type === 'RETRAIT')  return <TrendingUp    size={13} className="text-red-500 shrink-0" />;
  return <ArrowLeftRight size={13} className="text-blue-500 shrink-0" />;
};

const CompteCard = ({ compte }) => {
  const [open,   setOpen]   = useState(false);
  const [txs,    setTxs]    = useState([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded]  = useState(false);

  const toggleTxs = async () => {
    if (!open && !loaded) {
      setLoading(true);
      try {
        const res = await getTransactions(compte.id, 0, 20);
        const data = res.data;
        const items = data?.data?.content ?? data?.content ?? [];
        setTxs(Array.isArray(items) ? items : []);
        setLoaded(true);
      } catch {
        setTxs([]);
        setLoaded(true);
      } finally {
        setLoading(false);
      }
    }
    setOpen(o => !o);
  };

  return (
    <div className="border border-gray-200 rounded-xl overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3 bg-gray-50">
        <div>
          <p className="text-sm font-semibold text-gray-800">
            {TYPE_LABELS[compte.typeCompte] ?? compte.typeCompte}
          </p>
          <p className="text-xs text-gray-400 font-mono">{compte.numeroCompte}</p>
        </div>
        <div className="flex items-center gap-3">
          <StatutBadge s={compte.statut} />
          <p className="text-sm font-bold text-gray-700">
            {Number(compte.solde ?? 0).toLocaleString('fr-FR')} FCFA
          </p>
          <button onClick={toggleTxs}
            className="flex items-center gap-1 text-xs text-blue-600 hover:underline whitespace-nowrap">
            Transactions {open ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
          </button>
        </div>
      </div>

      {open && (
        <div className="px-4 py-3">
          {loading ? (
            <p className="text-sm text-gray-400 text-center py-4">Chargement…</p>
          ) : txs.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-3">Aucune transaction</p>
          ) : (
            <div className="divide-y divide-gray-50">
              {txs.map(tx => {
                const type = tx.typeTransaction ?? tx.type ?? '';
                const date = tx.dateTransaction
                  ? new Date(tx.dateTransaction).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
                  : '';
                return (
                  <div key={tx.id} className="flex items-center justify-between py-2 text-sm">
                    <div className="flex items-center gap-2 min-w-0">
                      <TxIcon type={type} />
                      <div className="min-w-0">
                        <p className="text-gray-700 font-medium">{type || '—'}</p>
                        {tx.description && (
                          <p className="text-xs text-gray-400 truncate max-w-xs">{tx.description}</p>
                        )}
                      </div>
                    </div>
                    <div className="text-right shrink-0 ml-4">
                      <p className={`font-semibold ${txColor(type)}`}>
                        {txPrefix(type)}{Number(tx.montant ?? 0).toLocaleString('fr-FR')} FCFA
                      </p>
                      <p className="text-xs text-gray-400">{date}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

const Row = ({ label, value }) => (
  <div className="flex justify-between text-sm py-1.5 border-b border-gray-50 last:border-0">
    <span className="text-gray-400">{label}</span>
    <span className="font-medium text-gray-700 text-right">{value}</span>
  </div>
);

const DirecteurClientDetailPage = () => {
  const { clientId } = useParams();
  const navigate     = useNavigate();

  const [client,    setClient]    = useState(null);
  const [comptes,   setComptes]   = useState([]);
  const [docs,      setDocs]      = useState([]);
  const [agentName, setAgentName] = useState(null);
  const [loading,   setLoading]   = useState(true);
  const [error,     setError]     = useState('');

  const [modalCompte,   setModalCompte]   = useState(false);
  const [compteForm,    setCompteForm]    = useState({ typeCompte: 'COURANT', soldeInitial: '', description: '' });
  const [savingCompte,  setSavingCompte]  = useState(false);
  const [compteError,   setCompteError]   = useState('');
  const [compteSuccess, setCompteSuccess] = useState('');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [cRes, acRes, agentsRes, docsRes] = await Promise.allSettled([
        getClientById(clientId),
        getComptesByClient(clientId, 0, 50),
        getMyAgencyAgents(),
        getClientDocuments(clientId),
      ]);

      let clientData = null;
      if (cRes.status === 'fulfilled') clientData = cRes.value.data;
      setClient(clientData);

      if (acRes.status === 'fulfilled') {
        const d = acRes.value.data;
        setComptes(d?.data?.content ?? d?.data ?? d?.content ?? []);
      }

      if (docsRes.status === 'fulfilled') {
        setDocs(docsRes.value.data ?? []);
      }

      if (agentsRes.status === 'fulfilled' && clientData?.createdBy) {
        const agents = agentsRes.value.data ?? [];
        const matched = agents.find(a => a.agentEmail === clientData.createdBy);
        setAgentName(matched
          ? `${matched.agentName} (${matched.agentEmail})`
          : clientData.createdBy);
      }
    } catch {
      setError('Impossible de charger les données du client.');
    } finally {
      setLoading(false);
    }
  }, [clientId]);

  useEffect(() => { load(); }, [load]);

  const handleOuvrirCompte = async (e) => {
    e.preventDefault();
    setSavingCompte(true); setCompteError('');
    try {
      await ouvrirCompte({
        clientId,
        typeCompte:   compteForm.typeCompte,
        soldeInitial: compteForm.soldeInitial ? parseFloat(compteForm.soldeInitial) : 0,
        description:  compteForm.description || null,
        clientEmail:  client?.email || null,
        clientNom:    client ? `${client.firstName} ${client.lastName}` : null,
      });
      setCompteSuccess('Compte créé avec succès — en attente de validation.');
      load();
    } catch (err) {
      setCompteError(err.response?.data?.message ?? 'Erreur lors de la création du compte.');
    } finally {
      setSavingCompte(false);
    }
  };

  if (loading) return <div className="p-6 text-gray-400 text-sm">Chargement…</div>;
  if (!client)  return <div className="p-6 text-red-500 text-sm">{error || 'Client introuvable.'}</div>;

  const totalSolde = comptes.reduce((s, c) => s + Number(c.solde ?? 0), 0);

  return (
    <div className="p-6 space-y-6">

      {/* En-tête */}
      <div className="flex items-center gap-3">
        <button onClick={() => navigate('/directeur/clients')}
          className="p-2 hover:bg-gray-100 rounded-lg text-gray-500 transition-colors">
          <ArrowLeft size={18} />
        </button>
        <div className="flex-1">
          <h1 className="text-2xl font-bold text-gray-800">
            {client.firstName} {client.lastName}
          </h1>
          <p className="text-gray-500 text-sm">{client.email} · {client.phoneNumber || '—'}</p>
        </div>
        <Badge status={client.status?.toLowerCase() || 'actif'} />
      </div>

      {error && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>}

      <div className="grid grid-cols-2 gap-6">

        {/* Informations personnelles */}
        <div className="card space-y-1">
          <div className="flex items-center gap-2 mb-3">
            <User size={15} className="text-blue-500" />
            <h3 className="font-semibold text-gray-700">Informations</h3>
          </div>
          <Row label="Adresse"      value={client.address || '—'} />
          <Row label="Type"         value={client.clientType === 'BUSINESS' ? 'Entreprise' : 'Individuel'} />
          <Row label="Score crédit" value={client.creditScore ?? '—'} />
          <Row label="Créé le"      value={client.createdAt ? new Date(client.createdAt).toLocaleDateString('fr-FR') : '—'} />
          {client.birthDate && (
            <Row label="Date de naissance" value={new Date(client.birthDate).toLocaleDateString('fr-FR')} />
          )}
        </div>

        {/* Agent responsable */}
        <div className="card space-y-1">
          <div className="flex items-center gap-2 mb-3">
            <UserCheck size={15} className="text-green-500" />
            <h3 className="font-semibold text-gray-700">Agent responsable</h3>
          </div>
          <div className="bg-green-50 rounded-xl px-4 py-3 text-sm">
            <p className="font-medium text-gray-800">
              {agentName || client.createdBy || 'Non assigné'}
            </p>
            <p className="text-xs text-gray-400 mt-0.5">Agent ayant créé ce client</p>
          </div>
          <div className="mt-2 space-y-1">
            <Row label="Nb comptes"    value={String(comptes.length)} />
            <Row label="Solde total"   value={`${totalSolde.toLocaleString('fr-FR')} FCFA`} />
          </div>
        </div>
      </div>

      {/* Comptes et transactions */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <CreditCard size={15} className="text-purple-500" />
            <h3 className="font-semibold text-gray-700">Comptes et Transactions</h3>
            <span className="text-xs text-gray-400">({comptes.length} compte(s))</span>
          </div>
          <button
            onClick={() => { setCompteForm({ typeCompte: 'COURANT', soldeInitial: '', description: '' }); setCompteError(''); setCompteSuccess(''); setModalCompte(true); }}
            className="btn-primary flex items-center gap-1 text-xs py-1.5 px-3">
            <PlusCircle size={13} /> Ouvrir un compte
          </button>
        </div>
        {comptes.length === 0 ? (
          <p className="text-sm text-gray-400 py-6 text-center">Aucun compte bancaire</p>
        ) : (
          <div className="space-y-3">
            {comptes.map(c => <CompteCard key={c.id} compte={c} />)}
          </div>
        )}
      </div>

      {/* Documents KYC */}
      <div className="card">
        <div className="flex items-center gap-2 mb-4">
          <FileText size={15} className="text-green-500" />
          <h3 className="font-semibold text-gray-700">Documents KYC</h3>
          <span className="text-xs text-gray-400">({docs.length} document(s))</span>
        </div>
        {docs.length === 0 ? (
          <p className="text-sm text-gray-400 py-6 text-center">Aucun document enregistré</p>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {docs.map(d => (
              <div key={d.id} className="flex items-center gap-3 bg-gray-50 rounded-lg p-3">
                <FileText size={20} className="text-gray-400 shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-700 truncate">{d.typeName ?? d.type}</p>
                  <p className="text-xs text-gray-400 truncate">{d.fileName}</p>
                  <p className="text-xs text-gray-400">
                    {d.uploadedAt ? new Date(d.uploadedAt).toLocaleDateString('fr-FR') : ''}
                  </p>
                </div>
                {d.fileUrl && (
                  <a href={d.fileUrl} target="_blank" rel="noreferrer"
                    className="p-1 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded shrink-0">
                    <Eye size={14} />
                  </a>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modal Ouvrir un Compte */}
      <Modal isOpen={modalCompte} onClose={() => setModalCompte(false)}
        title="Ouvrir un Compte Bancaire"
        subtitle={`Pour ${client.firstName} ${client.lastName}`}>
        {compteSuccess ? (
          <div className="space-y-4">
            <div className="bg-green-50 border border-green-200 rounded-xl p-4 text-sm text-green-700">
              <p className="font-semibold mb-1">Compte créé !</p>
              <p>{compteSuccess}</p>
            </div>
            <div className="flex justify-end">
              <button onClick={() => { setModalCompte(false); setCompteSuccess(''); }} className="btn-primary">Fermer</button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleOuvrirCompte} className="space-y-4 min-w-[360px]">
            <div>
              <label className="form-label">Type de compte *</label>
              <select value={compteForm.typeCompte}
                onChange={e => setCompteForm(p => ({ ...p, typeCompte: e.target.value }))}
                className="form-input">
                <option value="COURANT">Compte Courant</option>
                <option value="EPARGNE">Compte d'Épargne</option>
                <option value="MICRO_EPARGNE">Micro-Épargne</option>
                <option value="DEPOT_A_TERME">Dépôt à Terme</option>
                <option value="CREDIT">Compte Crédit</option>
              </select>
            </div>
            <div>
              <label className="form-label">Dépôt initial (FCFA)</label>
              <input type="number" min="0" step="any" value={compteForm.soldeInitial}
                onChange={e => setCompteForm(p => ({ ...p, soldeInitial: e.target.value }))}
                placeholder="0" className="form-input" />
            </div>
            <div>
              <label className="form-label">Description</label>
              <input value={compteForm.description}
                onChange={e => setCompteForm(p => ({ ...p, description: e.target.value }))}
                placeholder="Optionnel" className="form-input" />
            </div>
            {compteError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{compteError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setModalCompte(false)} className="btn-secondary">Annuler</button>
              <button type="submit" disabled={savingCompte} className="btn-primary disabled:opacity-60">
                {savingCompte ? 'Création…' : 'Créer le compte'}
              </button>
            </div>
          </form>
        )}
      </Modal>

    </div>
  );
};

export default DirecteurClientDetailPage;
