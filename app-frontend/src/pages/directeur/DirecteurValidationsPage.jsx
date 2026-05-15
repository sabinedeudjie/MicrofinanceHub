import React, { useState, useEffect } from 'react';
import { CheckCircle, XCircle, Eye, FileText, Clock, Users } from 'lucide-react';
import Modal from '../../components/common/Modal';
import { getMyAgency, getMyAgencyClients } from '../../api/agencyApi';
import { getClientById } from '../../api/clientsApi';
import { getComptesEnAttenteValidation, changerStatut } from '../../api/comptesApi';
import { getClientDocuments } from '../../api/documentsApi';

const statutBadge = (s) => {
  const map = {
    EN_ATTENTE_VALIDATION: 'bg-yellow-100 text-yellow-700',
    ACTIF:  'bg-green-100 text-green-700',
    REJETE: 'bg-red-100 text-red-700',
    FERME:  'bg-gray-100 text-gray-500',
  };
  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${map[s] ?? 'bg-gray-100 text-gray-500'}`}>
      {s === 'EN_ATTENTE_VALIDATION' ? 'En attente' : s.charAt(0) + s.slice(1).toLowerCase()}
    </span>
  );
};



const DirecteurValidationsPage = () => {
  const [agency,      setAgency]      = useState(null);
  const [pendingList, setPendingList] = useState([]);
  const [loading,      setLoading]      = useState(true);
  const [error,        setError]        = useState('');

  // modal détail / validation
  const [selected,     setSelected]     = useState(null); // compte sélectionné
  const [clientInfo,   setClientInfo]   = useState(null);
  const [documents,    setDocuments]    = useState([]);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [modalOpen,    setModalOpen]    = useState(false);
  const [validating,   setValidating]   = useState(false);
  const [actionError,  setActionError]  = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');

    // Récupérer l'agence (non bloquant si service indisponible)
    let ag = null;
    try {
      const agRes = await getMyAgency();
      ag = agRes.data;
      setAgency(ag);
    } catch {
      setError('Service agence indisponible — affichage de tous les dossiers en attente.');
    }

    try {
      const [agencyClientsRes, pendingRes] = await Promise.allSettled([
        ag ? getMyAgencyClients() : Promise.resolve({ data: { clients: [] } }),
        getComptesEnAttenteValidation(),
      ]);

      const agencyClientIds = ag
        ? new Set((agencyClientsRes.status === 'fulfilled'
            ? agencyClientsRes.value.data?.clients ?? []
            : []).map(c => c.clientId))
        : null;

      const allPending = pendingRes.status === 'fulfilled'
        ? (pendingRes.value.data?.data?.content ?? pendingRes.value.data?.data ?? [])
        : [];

      // Si agence connue, filtrer ; sinon afficher tout
      setPendingList(agencyClientIds ? allPending.filter(c => agencyClientIds.has(c.clientId)) : allPending);
    } catch {
      setError('Impossible de charger les comptes en attente.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openDetail = async (compte) => {
    setSelected(compte);
    setClientInfo(null);
    setDocuments([]);
    setActionError('');
    setActionSuccess('');
    setModalOpen(true);
    setLoadingDetail(true);
    try {
      const [cRes, dRes] = await Promise.allSettled([
        getClientById(compte.clientId),
        getClientDocuments(compte.clientId),
      ]);
      if (cRes.status === 'fulfilled') setClientInfo(cRes.value.data);
      if (dRes.status === 'fulfilled') setDocuments(dRes.value.data ?? []);
    } finally {
      setLoadingDetail(false);
    }
  };

  const handleValidate = async (statut) => {
    setValidating(true);
    setActionError('');
    try {
      await changerStatut(selected.id, statut);
      setActionSuccess(statut === 'ACTIF' ? 'Compte approuvé avec succès !' : 'Compte rejeté.');
      load();
    } catch (err) {
      setActionError(err.response?.data?.message ?? 'Erreur lors de la validation.');
    } finally {
      setValidating(false);
    }
  };

  return (
    <div className="p-6 space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Validations de Comptes</h1>
        <p className="text-gray-500 text-sm">
          {agency ? `${agency.code} — ${agency.name}` : 'Chargement…'}
        </p>
      </div>

      {error && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>}

      <div className="card">
        <div className="flex items-center gap-2 mb-4">
          <Clock size={16} className="text-yellow-500" />
          <h3 className="font-semibold text-gray-700">Comptes en attente de validation</h3>
          <span className="ml-auto text-sm text-gray-400">{loading ? '…' : `${pendingList.length} dossier(s)`}</span>
        </div>

        <table className="w-full text-sm">
          <thead>
            <tr className="text-xs text-gray-400 border-b border-gray-100">
              <th className="text-left pb-3">N° Compte</th>
              <th className="text-left pb-3">Type</th>
              <th className="text-left pb-3">Client ID</th>
              <th className="text-left pb-3">Dépôt initial</th>
              <th className="text-left pb-3">Date</th>
              <th className="text-left pb-3">Statut</th>
              <th className="text-left pb-3">Action</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="py-8 text-center text-gray-400">Chargement…</td></tr>
            ) : pendingList.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-12 text-center text-gray-300">
                  <Users size={32} className="mx-auto mb-2" />
                  <p className="text-sm">Aucun compte en attente de validation</p>
                </td>
              </tr>
            ) : (
              pendingList.map(c => (
                <tr key={c.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="py-3 font-mono text-xs text-gray-700">{c.numeroCompte}</td>
                  <td className="py-3 text-gray-600">{c.typeCompte}</td>
                  <td className="py-3 text-xs text-gray-400 font-mono">{c.clientId?.slice(0, 8)}…</td>
                  <td className="py-3 text-gray-600">
                    {new Intl.NumberFormat('fr-FR').format(c.solde)} XAF
                  </td>
                  <td className="py-3 text-xs text-gray-400">
                    {c.dateOuverture ? new Date(c.dateOuverture).toLocaleDateString('fr-FR') : '—'}
                  </td>
                  <td className="py-3">{statutBadge(c.statut)}</td>
                  <td className="py-3">
                    <button onClick={() => openDetail(c)}
                      className="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-800 font-medium">
                      <Eye size={13} /> Examiner
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* ── Modal détail ── */}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)}
        title="Dossier de validation"
        subtitle={selected?.numeroCompte}>

        {loadingDetail ? (
          <p className="text-gray-400 text-sm py-4 text-center">Chargement du dossier…</p>
        ) : actionSuccess ? (
          <div className="space-y-4">
            <div className="bg-green-50 border border-green-200 rounded-xl p-4 text-sm text-green-700 font-medium">
              {actionSuccess}
            </div>
            <div className="flex justify-end">
              <button onClick={() => { setModalOpen(false); setActionSuccess(''); }} className="btn-primary">Fermer</button>
            </div>
          </div>
        ) : (
          <div className="space-y-5">

            {/* Infos compte */}
            <div className="bg-gray-50 rounded-xl p-4 text-sm space-y-1">
              <p><span className="text-gray-400">N° compte :</span> <span className="font-mono font-medium">{selected?.numeroCompte}</span></p>
              <p><span className="text-gray-400">Type :</span> <span className="font-medium">{selected?.typeCompte}</span></p>
              <p><span className="text-gray-400">Dépôt initial :</span> <span className="font-medium">{new Intl.NumberFormat('fr-FR').format(selected?.solde ?? 0)} XAF</span></p>
            </div>

            {/* Infos client */}
            {clientInfo && (
              <div className="bg-blue-50 rounded-xl p-4 text-sm space-y-1">
                <p className="font-semibold text-blue-800 mb-2">{clientInfo.firstName} {clientInfo.lastName}</p>
                <p><span className="text-blue-500">Email :</span> {clientInfo.email}</p>
                <p><span className="text-blue-500">Téléphone :</span> {clientInfo.phoneNumber || '—'}</p>
                <p><span className="text-blue-500">Adresse :</span> {clientInfo.address || '—'}</p>
              </div>
            )}

            {/* Documents */}
            <div>
              <p className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
                <FileText size={14} /> Documents fournis ({documents.length})
              </p>
              {documents.length === 0 ? (
                <p className="text-sm text-yellow-600 bg-yellow-50 px-3 py-2 rounded-lg">
                  Aucun document fourni — examinez avec précaution.
                </p>
              ) : (
                <div className="space-y-2">
                  {documents.map(d => (
                    <div key={d.id} className="flex items-center justify-between bg-gray-50 rounded-lg px-3 py-2">
                      <div>
                        <p className="text-sm font-medium text-gray-700">{d.typeName}</p>
                        <p className="text-xs text-gray-400">{d.fileName}</p>
                      </div>
                      <div className="flex items-center gap-3">

                        <a href={d.fileUrl} target="_blank" rel="noreferrer"
                          className="text-xs text-blue-600 hover:underline flex items-center gap-1">
                          <Eye size={12} /> Voir
                        </a>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {actionError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{actionError}</p>}

            {/* Actions validation */}
            <div className="flex justify-end gap-3 pt-2 border-t border-gray-100">
              <button onClick={() => setModalOpen(false)} className="btn-secondary">Annuler</button>
              <button onClick={() => handleValidate('REJETE')} disabled={validating}
                className="flex items-center gap-2 px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg text-sm font-medium disabled:opacity-60">
                <XCircle size={15} /> Rejeter
              </button>
              <button onClick={() => handleValidate('ACTIF')} disabled={validating}
                className="flex items-center gap-2 px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg text-sm font-medium disabled:opacity-60">
                <CheckCircle size={15} /> Approuver
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default DirecteurValidationsPage;
