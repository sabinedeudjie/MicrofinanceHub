import React, { useState, useEffect, useCallback } from 'react';
import { FileText, TrendingDown, TrendingUp, Download, Search, Wallet } from 'lucide-react';
import Badge from '../../components/common/Badge';
import { getCurrentUser } from '../../utils/auth';
import { getComptesByClient } from '../../api/comptesApi';
import { getTransactions, getTransactionsByPeriode, getTransactionsByType } from '../../api/transactionsApi';
import { formatMontant, formatDate, formatTypeTransaction, formatTypeCompte, isCredit } from '../../utils/formatters';

const TYPES = ['Toutes', 'DEPOT', 'RETRAIT', 'VIREMENT_SORTANT', 'VIREMENT_ENTRANT'];

const TYPE_LABELS = {
  Toutes:           'Toutes',
  DEPOT:            'Dépôts',
  RETRAIT:          'Retraits',
  VIREMENT_SORTANT: 'Virements sortants',
  VIREMENT_ENTRANT: 'Virements entrants',
};

const TransactionsPage = () => {
  const [comptes, setComptes]           = useState([]);
  const [compteId, setCompteId]         = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loadingComptes, setLoadingComptes] = useState(true);
  const [loading, setLoading]           = useState(false);
  const [error, setError]               = useState(null);

  const [search,    setSearch]    = useState('');
  const [filtre,    setFiltre]    = useState('Toutes');
  const [dateDebut, setDateDebut] = useState('');
  const [dateFin,   setDateFin]   = useState('');

  // Charger tous les comptes du client
  useEffect(() => {
    const user = getCurrentUser();
    if (!user?.id) { setLoadingComptes(false); return; }

    getComptesByClient(user.clientId ?? user.id, 0, 20)
      .then(res => {
        const liste = res.data.data.content ?? [];
        setComptes(liste);
        if (liste.length > 0) setCompteId(liste[0].id);
      })
      .catch(() => setError('Impossible de charger vos comptes. Vérifiez que account-service est démarré (port 8082).'))
      .finally(() => setLoadingComptes(false));
  }, []);

  // Charger les transactions selon les filtres actifs
  const fetchTransactions = useCallback(async () => {
    if (!compteId) return;
    setLoading(true);
    setError(null);
    try {
      let res;
      if (dateDebut && dateFin) {
        res = await getTransactionsByPeriode(
          compteId,
          new Date(dateDebut).toISOString(),
          new Date(dateFin + 'T23:59:59').toISOString()
        );
      } else if (filtre !== 'Toutes') {
        res = await getTransactionsByType(compteId, filtre, 0, 50);
      } else {
        res = await getTransactions(compteId, 0, 50);
      }
      setTransactions(res.data.data.content ?? []);
    } catch {
      setError('Impossible de charger les transactions.');
    } finally {
      setLoading(false);
    }
  }, [compteId, dateDebut, dateFin, filtre]);

  useEffect(() => { fetchTransactions(); }, [fetchTransactions]);

  const transactionsFiltrees = transactions.filter(t =>
    !search ||
    t.reference?.toLowerCase().includes(search.toLowerCase()) ||
    t.description?.toLowerCase().includes(search.toLowerCase())
  );

  const compteSelectionne = comptes.find(c => c.id === compteId);

  const handleDownloadPDF = () => {
    const compte = compteSelectionne;
    const lignes = transactionsFiltrees.map(t => {
      const credit = isCredit(t.typeTransaction);
      return `
        <tr>
          <td>${t.reference ?? '—'}</td>
          <td>${formatTypeTransaction(t.typeTransaction)}</td>
          <td>${t.description ?? '—'}</td>
          <td style="color:${credit ? '#16a34a' : '#dc2626'};font-weight:600">
            ${credit ? '+' : '-'}${formatMontant(t.montant)} FCFA
          </td>
          <td>${formatMontant(t.soldeApres)} FCFA</td>
          <td>${formatDate(t.dateTransaction)}</td>
        </tr>`;
    }).join('');

    const html = `<!DOCTYPE html><html><head><meta charset="UTF-8">
      <title>Relevé — ${compte?.numeroCompte ?? ''}</title>
      <style>
        body { font-family: Arial, sans-serif; font-size: 12px; margin: 30px; color: #111; }
        h1 { font-size: 18px; margin-bottom: 4px; }
        .sub { color: #555; font-size: 12px; margin-bottom: 20px; }
        table { width: 100%; border-collapse: collapse; margin-top: 12px; }
        th { background: #1e3a5f; color: #fff; padding: 8px 6px; text-align: left; font-size: 11px; }
        td { padding: 6px; border-bottom: 1px solid #e5e7eb; vertical-align: top; }
        tr:nth-child(even) td { background: #f9fafb; }
        .footer { margin-top: 24px; font-size: 10px; color: #888; text-align: center; }
        @media print { body { margin: 15px; } }
      </style></head><body>
      <h1>Relevé de compte — MicroFinanceHub</h1>
      <div class="sub">
        Compte : <strong>${compte?.numeroCompte ?? '—'}</strong> &nbsp;|&nbsp;
        Type : <strong>${formatTypeCompte(compte?.typeCompte)}</strong> &nbsp;|&nbsp;
        Solde actuel : <strong>${formatMontant(compte?.solde)} FCFA</strong><br>
        Généré le : ${new Date().toLocaleDateString('fr-FR', { day:'2-digit', month:'long', year:'numeric' })}
      </div>
      <table>
        <thead><tr>
          <th>Référence</th><th>Type</th><th>Description</th>
          <th>Montant</th><th>Solde après</th><th>Date</th>
        </tr></thead>
        <tbody>${lignes}</tbody>
      </table>
      <div class="footer">MicroFinanceHub — Document généré automatiquement</div>
      <script>window.onload = function(){ window.print(); }<\/script>
    </body></html>`;

    const win = window.open('', '_blank');
    win.document.write(html);
    win.document.close();
  };

  const handleSelectCompte = (id) => {
    setCompteId(id);
    setFiltre('Toutes');
    setDateDebut('');
    setDateFin('');
    setSearch('');
  };

  return (
    <div className="p-6 space-y-5">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Mes Transactions</h1>
          <p className="text-gray-500 text-sm">
            {loading ? 'Chargement…' : `${transactionsFiltrees.length} opération(s) affichée(s)`}
          </p>
        </div>
        <button
          onClick={handleDownloadPDF}
          disabled={!compteId || transactionsFiltrees.length === 0}
          className="btn-secondary flex items-center gap-2 disabled:opacity-50"
        >
          <Download size={15} /> Télécharger Relevé PDF
        </button>
      </div>

      {/* Sélecteur de compte */}
      {loadingComptes ? (
        <div className="card py-4 text-center text-gray-400 text-sm">Chargement des comptes…</div>
      ) : comptes.length === 0 ? (
        <div className="card py-6 text-center text-gray-400 text-sm">
          <Wallet size={28} className="mx-auto mb-2 text-gray-300" />
          Aucun compte trouvé
        </div>
      ) : (
        <div className="card">
          <p className="text-xs text-gray-400 font-medium mb-3 uppercase tracking-wide">Sélectionner un compte</p>
          <div className="flex gap-3 flex-wrap">
            {comptes.map(c => (
              <button
                key={c.id}
                onClick={() => handleSelectCompte(c.id)}
                className={`flex-1 min-w-48 text-left px-4 py-3 rounded-xl border-2 transition-all ${
                  c.id === compteId
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-100 bg-gray-50 hover:border-gray-300'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className={`text-xs font-semibold ${c.id === compteId ? 'text-blue-600' : 'text-gray-500'}`}>
                    {formatTypeCompte(c.typeCompte)}
                  </span>
                  <Badge status={c.statut} />
                </div>
                <p className="text-xs font-mono text-gray-400">{c.numeroCompte}</p>
                <p className={`text-sm font-bold mt-1 ${c.id === compteId ? 'text-blue-700' : 'text-gray-700'}`}>
                  {formatMontant(c.solde)} FCFA
                </p>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Filtres */}
      {compteId && (
        <div className="card">
          <div className="flex items-center gap-4 flex-wrap">
            <div className="relative flex-1 min-w-48">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input value={search} onChange={e => setSearch(e.target.value)}
                placeholder="Rechercher par référence ou description…" className="form-input pl-9" />
            </div>
            <div className="flex gap-1 bg-gray-100 p-1 rounded-lg flex-wrap">
              {TYPES.map(t => (
                <button key={t} onClick={() => setFiltre(t)}
                  className={`px-3 py-1.5 text-xs rounded-md transition-colors font-medium ${
                    filtre === t ? 'bg-white text-gray-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'
                  }`}>
                  {TYPE_LABELS[t]}
                </button>
              ))}
            </div>
            <input type="date" value={dateDebut} onChange={e => setDateDebut(e.target.value)}
              className="form-input text-sm" />
            <span className="text-gray-400 text-sm">→</span>
            <input type="date" value={dateFin} onChange={e => setDateFin(e.target.value)}
              className="form-input text-sm" />
          </div>
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">{error}</div>
      )}

      {/* Tableau */}
      {compteId && (
        <div className="card">
          {compteSelectionne && (
            <p className="text-xs text-gray-400 mb-4">
              Compte <span className="font-mono font-medium text-gray-600">{compteSelectionne.numeroCompte}</span>
              {' '}— {formatTypeCompte(compteSelectionne.typeCompte)}
            </p>
          )}
          {loading ? (
            <p className="text-center text-gray-400 py-10 text-sm">Chargement des transactions…</p>
          ) : transactionsFiltrees.length === 0 ? (
            <div className="flex flex-col items-center gap-2 py-10 text-gray-400 text-sm">
              <FileText size={32} className="text-gray-300" />
              Aucune transaction trouvée
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-xs text-gray-400 border-b border-gray-100">
                  <th className="text-left pb-3">Référence</th>
                  <th className="text-left pb-3">Type</th>
                  <th className="text-left pb-3">Description</th>
                  <th className="text-left pb-3">Montant</th>
                  <th className="text-left pb-3">Solde après</th>
                  <th className="text-left pb-3">Date</th>
                  <th className="text-left pb-3">Statut</th>
                </tr>
              </thead>
              <tbody>
                {transactionsFiltrees.map(t => {
                  const credit = isCredit(t.typeTransaction);
                  return (
                    <tr key={t.id} className="border-b border-gray-50 hover:bg-gray-50">
                      <td className="py-3 text-xs font-mono text-gray-500">{t.reference}</td>
                      <td className="py-3">
                        <span className={`flex items-center gap-1.5 text-xs font-medium ${credit ? 'text-green-600' : 'text-red-500'}`}>
                          {credit ? <TrendingDown size={12} /> : <TrendingUp size={12} />}
                          {formatTypeTransaction(t.typeTransaction)}
                        </span>
                      </td>
                      <td className="py-3 text-gray-600 max-w-xs truncate">{t.description || '—'}</td>
                      <td className={`py-3 font-bold ${credit ? 'text-green-600' : 'text-red-500'}`}>
                        {credit ? '+' : '-'}{formatMontant(t.montant)} FCFA
                      </td>
                      <td className="py-3 text-gray-500 text-xs">{formatMontant(t.soldeApres)} FCFA</td>
                      <td className="py-3 text-gray-400 text-xs">{formatDate(t.dateTransaction)}</td>
                      <td className="py-3"><Badge status={t.statut} /></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
};

export default TransactionsPage;
