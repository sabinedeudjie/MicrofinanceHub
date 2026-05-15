import React, { useState, useEffect, useCallback } from 'react';
import { ArrowLeftRight, TrendingDown, TrendingUp, Search, RefreshCw } from 'lucide-react';
import Badge from '../../components/common/Badge';
import { rechercherTransactions, relancerTransactions } from '../../api/transactionsApi';
import { formatMontant, formatDate, formatTypeTransaction, isCredit } from '../../utils/formatters';

const STATUTS = ['', 'COMPLETEE', 'EN_TRAITEMENT', 'ECHOUEE', 'ANNULEE'];
const TYPES   = ['', 'DEPOT', 'RETRAIT', 'VIREMENT_SORTANT', 'VIREMENT_ENTRANT'];

const STATUT_LABELS = {
  '': 'Tous statuts', COMPLETEE: 'Complétées', EN_TRAITEMENT: 'En traitement',
  ECHOUEE: 'Échouées', ANNULEE: 'Annulées',
};
const TYPE_LABELS = {
  '': 'Tous types', DEPOT: 'Dépôts', RETRAIT: 'Retraits',
  VIREMENT_SORTANT: 'Virements sortants', VIREMENT_ENTRANT: 'Virements entrants',
};

const TransactionsAdminPage = () => {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading]           = useState(true);
  const [error, setError]               = useState(null);
  const [relancing, setRelancing]       = useState(false);

  const [search,    setSearch]    = useState('');
  const [statut,    setStatut]    = useState('');
  const [type,      setType]      = useState('');
  const [compteId,  setCompteId]  = useState('');
  const [dateDebut, setDateDebut] = useState('');
  const [dateFin,   setDateFin]   = useState('');

  const fetchTransactions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = { page: 0, size: 100 };
      if (statut)   params.statut          = statut;
      if (type)     params.typeTransaction  = type;
      if (compteId) params.compteId         = Number(compteId);
      if (search)   params.reference        = search;
      if (dateDebut) params.debut = new Date(dateDebut).toISOString();
      if (dateFin)   params.fin   = new Date(dateFin + 'T23:59:59').toISOString();

      const res = await rechercherTransactions(params);
      setTransactions(res.data.data.content ?? []);
    } catch {
      setError('Impossible de charger les transactions. Vérifiez que transaction-service est démarré (port 8088).');
    } finally {
      setLoading(false);
    }
  }, [statut, type, compteId, search, dateDebut, dateFin]);

  useEffect(() => { fetchTransactions(); }, [fetchTransactions]);

  const handleRelancer = async () => {
    setRelancing(true);
    try {
      const res = await relancerTransactions();
      const n = res.data.data ?? 0;
      if (n > 0) {
        alert(`✅ ${n} transaction(s) mise(s) à jour depuis CamPay.`);
      } else {
        alert('ℹ️ Aucune transaction à mettre à jour.\n\nSi votre transaction est toujours EN_TRAITEMENT :\n1. Connectez-vous sur demo.campay.net\n2. Approuvez la transaction dans le tableau de bord\n3. Cliquez à nouveau sur "Synchroniser CamPay"');
      }
      fetchTransactions();
    } catch {
      alert('Erreur lors de la synchronisation CamPay');
    } finally {
      setRelancing(false);
    }
  };

  return (
    <div className="p-6 space-y-5">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Transactions</h1>
          <p className="text-gray-500 text-sm">
            {loading ? 'Chargement…' : `${transactions.length} transaction(s) affichée(s)`}
          </p>
        </div>
        <button
          onClick={handleRelancer}
          disabled={relancing}
          title="Interroge CamPay pour mettre à jour les transactions EN_TRAITEMENT. À utiliser en sandbox après validation sur demo.campay.net."
          className="btn-secondary flex items-center gap-2"
        >
          <RefreshCw size={15} className={relancing ? 'animate-spin' : ''} />
          {relancing ? 'Synchronisation…' : 'Synchroniser CamPay'}
        </button>
      </div>

      {/* Filtres */}
      <div className="card">
        <div className="flex items-center gap-3 flex-wrap">
          <div className="relative flex-1 min-w-48">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Référence…"
              className="form-input pl-9"
            />
          </div>
          <input
            type="number"
            value={compteId}
            onChange={e => setCompteId(e.target.value)}
            placeholder="ID Compte"
            className="form-input w-32"
          />
          <select value={type} onChange={e => setType(e.target.value)} className="form-input">
            {TYPES.map(t => <option key={t} value={t}>{TYPE_LABELS[t]}</option>)}
          </select>
          <select value={statut} onChange={e => setStatut(e.target.value)} className="form-input">
            {STATUTS.map(s => <option key={s} value={s}>{STATUT_LABELS[s]}</option>)}
          </select>
          <input type="date" value={dateDebut} onChange={e => setDateDebut(e.target.value)} className="form-input text-sm" />
          <span className="text-gray-400">→</span>
          <input type="date" value={dateFin} onChange={e => setDateFin(e.target.value)} className="form-input text-sm" />
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">{error}</div>
      )}

      {/* Bannière sandbox pour transactions EN_TRAITEMENT */}
      {!loading && transactions.some(t => t.statut === 'EN_TRAITEMENT') && (
        <div className="bg-amber-50 border border-amber-200 rounded-lg px-4 py-3 text-sm text-amber-800 flex items-start gap-3">
          <RefreshCw size={16} className="mt-0.5 shrink-0 text-amber-600" />
          <div>
            <p className="font-medium mb-1">Des transactions sont en attente de confirmation CamPay</p>
            <p className="text-amber-700 text-xs">
              En sandbox : connectez-vous sur <strong>demo.campay.net</strong>, trouvez la transaction via
              la référence CamPay affichée, approuvez-la, puis cliquez sur <strong>Synchroniser CamPay</strong>.
            </p>
          </div>
        </div>
      )}

      {/* Tableau */}
      <div className="card">
        {loading ? (
          <p className="text-center text-gray-400 py-10 text-sm">Chargement des transactions…</p>
        ) : transactions.length === 0 ? (
          <div className="flex flex-col items-center gap-2 py-10 text-gray-400 text-sm">
            <ArrowLeftRight size={32} className="text-gray-300" />
            Aucune transaction trouvée
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-400 border-b border-gray-100">
                <th className="text-left pb-3">Référence interne</th>
                <th className="text-left pb-3">Réf. CamPay</th>
                <th className="text-left pb-3">Compte</th>
                <th className="text-left pb-3">Type</th>
                <th className="text-left pb-3">Montant</th>
                <th className="text-left pb-3">Solde après</th>
                <th className="text-left pb-3">Mode</th>
                <th className="text-left pb-3">Date</th>
                <th className="text-left pb-3">Statut</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map(t => {
                const credit = isCredit(t.typeTransaction);
                return (
                  <tr key={t.id} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 text-xs font-mono text-gray-500">{t.reference}</td>
                    <td className="py-3 text-xs font-mono">
                      {t.campayReference
                        ? <span className="text-blue-600" title={t.campayReference}>{t.campayReference.slice(0, 12)}…</span>
                        : <span className="text-gray-300">—</span>}
                    </td>
                    <td className="py-3 text-xs text-gray-500">{t.compteId}</td>
                    <td className="py-3">
                      <span className={`flex items-center gap-1.5 text-xs font-medium ${credit ? 'text-green-600' : 'text-red-500'}`}>
                        {credit ? <TrendingDown size={12} /> : <TrendingUp size={12} />}
                        {formatTypeTransaction(t.typeTransaction)}
                      </span>
                    </td>
                    <td className={`py-3 font-bold ${credit ? 'text-green-600' : 'text-red-500'}`}>
                      {credit ? '+' : '-'}{formatMontant(t.montant)} FCFA
                    </td>
                    <td className="py-3 text-gray-500 text-xs">{formatMontant(t.soldeApres)} FCFA</td>
                    <td className="py-3 text-gray-400 text-xs">{t.modePaiement || '—'}</td>
                    <td className="py-3 text-gray-400 text-xs">{formatDate(t.dateTransaction)}</td>
                    <td className="py-3"><Badge status={t.statut} /></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default TransactionsAdminPage;
