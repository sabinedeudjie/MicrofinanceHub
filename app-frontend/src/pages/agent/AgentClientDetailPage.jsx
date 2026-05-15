import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft, User, CreditCard, FileText, Upload,
  PlusCircle, Eye, ChevronDown, ChevronUp,
  TrendingDown, TrendingUp, ArrowLeftRight,
} from 'lucide-react';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import { getClientById } from '../../api/clientsApi';
import { getComptesByClient, ouvrirCompte } from '../../api/comptesApi';
import { getClientDocuments, uploadDocument } from '../../api/documentsApi';
import { getTransactions } from '../../api/transactionsApi';

const DOC_TYPES = [
  { value: 'ID_CARD',               label: "Carte d'identité" },
  { value: 'PASSPORT',              label: 'Passeport' },
  { value: 'DRIVER_LICENSE',        label: 'Permis de conduire' },
  { value: 'PROOF_OF_ADDRESS',      label: 'Justificatif de domicile' },
  { value: 'BUSINESS_REGISTRATION', label: 'Registre de commerce' },
  { value: 'TAX_IDENTIFICATION',    label: 'Numéro fiscal (TIN)' },
  { value: 'BANK_STATEMENT',        label: 'Relevé bancaire' },
  { value: 'PHOTO',                 label: "Photo d'identité" },
];

const COMPTE_TYPES = [
  { value: 'COURANT',       label: 'Compte Courant' },
  { value: 'EPARGNE',       label: "Compte d'Épargne" },
  { value: 'MICRO_EPARGNE', label: 'Micro-Épargne' },
  { value: 'DEPOT_A_TERME', label: 'Dépôt à Terme' },
  { value: 'CREDIT',        label: 'Compte Crédit' },
];

const statutBadge = (s) => {
  const map = {
    EN_ATTENTE_VALIDATION: { color: 'bg-yellow-100 text-yellow-700', label: 'En attente' },
    ACTIF:   { color: 'bg-green-100 text-green-700',  label: 'Actif' },
    BLOQUE:  { color: 'bg-red-100 text-red-700',      label: 'Bloqué' },
    FERME:   { color: 'bg-gray-100 text-gray-500',    label: 'Fermé' },
    REJETE:  { color: 'bg-red-100 text-red-700',      label: 'Rejeté' },
  };
  const cfg = map[s] ?? { color: 'bg-gray-100 text-gray-500', label: s };
  return <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${cfg.color}`}>{cfg.label}</span>;
};



const TX_COLOR  = { DEPOT: 'text-green-600', RETRAIT: 'text-red-500' };
const TX_PREFIX = { DEPOT: '+', RETRAIT: '-' };
const TxIcon = ({ type }) => {
  if (type === 'DEPOT')   return <TrendingDown  size={13} className="text-green-500 shrink-0" />;
  if (type === 'RETRAIT') return <TrendingUp    size={13} className="text-red-500 shrink-0" />;
  return <ArrowLeftRight size={13} className="text-blue-500 shrink-0" />;
};

const CompteCard = ({ c, onOuvrir }) => {
  const [open,    setOpen]   = useState(false);
  const [txs,     setTxs]    = useState([]);
  const [loading, setLoad]   = useState(false);
  const [loaded,  setLoaded] = useState(false);

  const toggle = async () => {
    if (!open && !loaded) {
      setLoad(true);
      try {
        const res = await getTransactions(c.id, 0, 20);
        const d   = res.data;
        const items = d?.data?.content ?? d?.content ?? [];
        setTxs(Array.isArray(items) ? items : []);
      } catch { setTxs([]); }
      finally { setLoad(false); setLoaded(true); }
    }
    setOpen(o => !o);
  };

  return (
    <div className="border border-gray-200 rounded-xl overflow-hidden">
      <div className="flex items-center justify-between px-3 py-2.5 bg-gray-50">
        <div>
          <p className="text-sm font-medium text-gray-800">{c.numeroCompte}</p>
          <p className="text-xs text-gray-400">{c.typeCompte}</p>
        </div>
        <div className="flex items-center gap-2">
          {statutBadge(c.statut)}
          <span className="text-xs font-semibold text-gray-700">
            {new Intl.NumberFormat('fr-FR').format(c.solde)} XAF
          </span>
          <button onClick={toggle}
            className="flex items-center gap-0.5 text-xs text-blue-600 hover:underline ml-1">
            Transactions {open ? <ChevronUp size={11} /> : <ChevronDown size={11} />}
          </button>
        </div>
      </div>

      {open && (
        <div className="px-3 py-2">
          {loading ? (
            <p className="text-xs text-gray-400 text-center py-3">Chargement…</p>
          ) : txs.length === 0 ? (
            <p className="text-xs text-gray-400 text-center py-3">Aucune transaction</p>
          ) : (
            <div className="divide-y divide-gray-50">
              {txs.map(tx => {
                const type = tx.typeTransaction ?? tx.type ?? '';
                const date = tx.dateTransaction
                  ? new Date(tx.dateTransaction).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
                  : '';
                return (
                  <div key={tx.id} className="flex items-center justify-between py-1.5 text-xs">
                    <div className="flex items-center gap-1.5 min-w-0">
                      <TxIcon type={type} />
                      <div className="min-w-0">
                        <p className="text-gray-700 font-medium">{type || '—'}</p>
                        {tx.description && (
                          <p className="text-gray-400 truncate max-w-[180px]">{tx.description}</p>
                        )}
                      </div>
                    </div>
                    <div className="text-right shrink-0 ml-3">
                      <p className={`font-semibold ${TX_COLOR[type] ?? 'text-blue-600'}`}>
                        {TX_PREFIX[type] ?? ''}{new Intl.NumberFormat('fr-FR').format(tx.montant ?? 0)} XAF
                      </p>
                      <p className="text-gray-400">{date}</p>
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

const AgentClientDetailPage = () => {
  const { clientId } = useParams();
  const navigate = useNavigate();

  const [client,   setClient]   = useState(null);
  const [comptes,  setComptes]  = useState([]);
  const [docs,     setDocs]     = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState('');

  // modal compte
  const [modalCompte, setModalCompte] = useState(false);
  const [compteForm,  setCompteForm]  = useState({ typeCompte: 'COURANT', soldeInitial: '', description: '' });
  const [savingCompte, setSavingCompte] = useState(false);
  const [compteError,  setCompteError]  = useState('');
  const [compteSuccess, setCompteSuccess] = useState('');

  // modal document
  const [modalDoc,    setModalDoc]    = useState(false);
  const [docForm,     setDocForm]     = useState({ type: 'ID_CARD', file: null });
  const [savingDoc,   setSavingDoc]   = useState(false);
  const [docError,    setDocError]    = useState('');
  const [docSuccess,  setDocSuccess]  = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [cRes, acRes, dRes] = await Promise.allSettled([
        getClientById(clientId),
        getComptesByClient(clientId),
        getClientDocuments(clientId),
      ]);
      if (cRes.status === 'fulfilled')  setClient(cRes.value.data);
      if (acRes.status === 'fulfilled') setComptes(acRes.value.data?.data?.content ?? acRes.value.data?.data ?? []);
      if (dRes.status === 'fulfilled')  setDocs(dRes.value.data ?? []);
    } catch {
      setError('Impossible de charger les données du client.');
    } finally {
      setLoading(false);
    }
  }, [clientId]);

  useEffect(() => { load(); }, [load]);

  // ── Ouvrir un compte ───────────────────────────────────────────────────────
  const handleOuvrirCompte = async (e) => {
    e.preventDefault();
    setSavingCompte(true);
    setCompteError('');
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

  // ── Upload document ────────────────────────────────────────────────────────
  const handleUploadDoc = async (e) => {
    e.preventDefault();
    if (!docForm.file) { setDocError('Sélectionnez un fichier.'); return; }
    setSavingDoc(true);
    setDocError('');
    try {
      await uploadDocument(clientId, docForm.file, docForm.type);
      setDocSuccess('Document ajouté avec succès.');
      load();
      setDocForm({ type: 'ID_CARD', file: null });
    } catch (err) {
      setDocError(err.response?.data?.message ?? "Erreur lors de l'upload.");
    } finally {
      setSavingDoc(false);
    }
  };

  if (loading) return <div className="p-6 text-gray-400">Chargement...</div>;
  if (!client) return <div className="p-6 text-red-500">{error || 'Client introuvable.'}</div>;

  return (
    <div className="p-6 space-y-6">

      {/* Header */}
      <div className="flex items-center gap-3">
        <button onClick={() => navigate('/agent/clients')}
          className="p-2 hover:bg-gray-100 rounded-lg text-gray-500">
          <ArrowLeft size={18} />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            {client.firstName} {client.lastName}
          </h1>
          <p className="text-gray-500 text-sm">{client.email} · {client.phoneNumber || '—'}</p>
        </div>
        <div className="ml-auto">
          <Badge status={client.status?.toLowerCase() || 'actif'} />
        </div>
      </div>

      {error && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>}

      <div className="grid grid-cols-2 gap-6">

        {/* ── Infos client ── */}
        <div className="card space-y-3">
          <div className="flex items-center gap-2 mb-1">
            <User size={16} className="text-blue-500" />
            <h3 className="font-semibold text-gray-700">Informations</h3>
          </div>
          <Row label="Adresse"       value={client.address || '—'} />
          <Row label="Date naissance" value={client.birthDate ? new Date(client.birthDate).toLocaleDateString('fr-FR') : '—'} />
          <Row label="Type"          value={client.clientType === 'BUSINESS' ? 'Entreprise' : 'Individuel'} />
          <Row label="Score crédit"  value={client.creditScore ?? '—'} />
          <Row label="Agent"         value={client.createdBy || '—'} />
          <Row label="Créé le"       value={client.createdAt ? new Date(client.createdAt).toLocaleDateString('fr-FR') : '—'} />
        </div>

        {/* ── Comptes bancaires ── */}
        <div className="card space-y-3">
          <div className="flex items-center justify-between mb-1">
            <div className="flex items-center gap-2">
              <CreditCard size={16} className="text-purple-500" />
              <h3 className="font-semibold text-gray-700">Comptes bancaires</h3>
            </div>
            <button onClick={() => { setCompteForm({ typeCompte: 'COURANT', soldeInitial: '', description: '' }); setCompteError(''); setCompteSuccess(''); setModalCompte(true); }}
              className="btn-primary flex items-center gap-1 text-xs py-1.5 px-3">
              <PlusCircle size={13} /> Ouvrir un compte
            </button>
          </div>

          {comptes.length === 0 ? (
            <p className="text-sm text-gray-400 py-4 text-center">Aucun compte bancaire</p>
          ) : (
            <div className="space-y-2">
              {comptes.map(c => <CompteCard key={c.id} c={c} />)}
            </div>
          )}
        </div>
      </div>

      {/* ── Documents KYC ── */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <FileText size={16} className="text-green-500" />
            <h3 className="font-semibold text-gray-700">Documents KYC</h3>
            <span className="text-xs text-gray-400">({docs.length} document(s))</span>
          </div>
          <button onClick={() => { setDocForm({ type: 'ID_CARD', file: null }); setDocError(''); setDocSuccess(''); setModalDoc(true); }}
            className="btn-primary flex items-center gap-1 text-xs py-1.5 px-3">
            <Upload size={13} /> Ajouter un document
          </button>
        </div>

        {docs.length === 0 ? (
          <p className="text-sm text-gray-400 py-6 text-center">Aucun document — ajoutez les pièces justificatives du client</p>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {docs.map(d => (
              <div key={d.id} className="flex items-center gap-3 bg-gray-50 rounded-lg p-3">
                <FileText size={20} className="text-gray-400 shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-700 truncate">{d.typeName}</p>
                  <p className="text-xs text-gray-400 truncate">{d.fileName}</p>
                  <p className="text-xs text-gray-400">
                    {d.uploadedAt ? new Date(d.uploadedAt).toLocaleDateString('fr-FR') : ''}
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">

                  <a href={d.fileUrl} target="_blank" rel="noreferrer"
                    className="p-1 hover:bg-blue-50 text-gray-400 hover:text-blue-600 rounded">
                    <Eye size={14} />
                  </a>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ── Modal Ouvrir Compte ────────────────────────────────────────────── */}
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
          <form onSubmit={handleOuvrirCompte} className="space-y-4">
            <div>
              <label className="form-label">Type de compte *</label>
              <select value={compteForm.typeCompte}
                onChange={e => setCompteForm(p => ({ ...p, typeCompte: e.target.value }))}
                className="form-input">
                {COMPTE_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>
            <div>
              <label className="form-label">Dépôt initial (XAF)</label>
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
                {savingCompte ? 'Création...' : 'Créer le compte'}
              </button>
            </div>
          </form>
        )}
      </Modal>

      {/* ── Modal Upload Document ──────────────────────────────────────────── */}
      <Modal isOpen={modalDoc} onClose={() => setModalDoc(false)}
        title="Ajouter un Document"
        subtitle="Pièce justificative fournie par le client">
        {docSuccess ? (
          <div className="space-y-4">
            <div className="bg-green-50 border border-green-200 rounded-xl p-4 text-sm text-green-700">
              <p className="font-semibold">Document ajouté !</p>
            </div>
            <div className="flex justify-end gap-3">
              <button onClick={() => { setDocSuccess(''); }}
                className="btn-secondary">Ajouter un autre</button>
              <button onClick={() => { setModalDoc(false); setDocSuccess(''); }} className="btn-primary">Fermer</button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleUploadDoc} className="space-y-4">
            <div>
              <label className="form-label">Type de document *</label>
              <select value={docForm.type}
                onChange={e => setDocForm(p => ({ ...p, type: e.target.value }))}
                className="form-input">
                {DOC_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>
            <div>
              <label className="form-label">Fichier *</label>
              <input type="file"
                accept=".jpg,.jpeg,.png,.pdf"
                onChange={e => setDocForm(p => ({ ...p, file: e.target.files[0] }))}
                className="form-input file:mr-3 file:py-1 file:px-3 file:rounded file:border-0 file:text-sm file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100" />
              <p className="text-xs text-gray-400 mt-1">JPG, PNG ou PDF · max 10 Mo</p>
            </div>
            {docError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{docError}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setModalDoc(false)} className="btn-secondary">Annuler</button>
              <button type="submit" disabled={savingDoc} className="btn-primary disabled:opacity-60">
                {savingDoc ? 'Upload...' : 'Téléverser'}
              </button>
            </div>
          </form>
        )}
      </Modal>

    </div>
  );
};

const Row = ({ label, value }) => (
  <div className="flex justify-between text-sm">
    <span className="text-gray-400">{label}</span>
    <span className="font-medium text-gray-700">{value}</span>
  </div>
);

export default AgentClientDetailPage;
