import React, { useState } from 'react';
import { BarChart2 } from 'lucide-react';
import { getLoanStats, getPortfolioStatsForClients, getLoanStatsForClients } from '../../api/loansApi';
import { getMyAgencyClientsStats, getMyAgencyClients } from '../../api/agencyApi';

const DirecteurRapportsPage = () => {
  const today = new Date().toISOString().split('T')[0];
  const firstOfMonth = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0];

  const [startDate, setStartDate] = useState(firstOfMonth);
  const [endDate, setEndDate]     = useState(today);
  const [loading, setLoading]     = useState(false);
  const [report, setReport]       = useState(null);
  const [error, setError]         = useState('');

  const handleGenerate = async () => {
    setLoading(true);
    setError('');
    try {
      const agencyRes = await getMyAgencyClients().catch(() => null);
      const clientIds = (agencyRes?.data?.clients ?? []).map(c => c.clientId);

      const [ls, ps, cs] = await Promise.allSettled([
        clientIds.length > 0
          ? getLoanStatsForClients(clientIds, startDate + 'T00:00:00', endDate + 'T23:59:59')
          : getLoanStats(startDate + 'T00:00:00', endDate + 'T23:59:59'),
        clientIds.length > 0
          ? getPortfolioStatsForClients(clientIds)
          : Promise.resolve({ data: null }),
        getMyAgencyClientsStats(),
      ]);
      setReport({
        loans:     ls.status === 'fulfilled' ? ls.value.data : null,
        portfolio: ps.status === 'fulfilled' ? ps.value.data : null,
        clients:   cs.status === 'fulfilled' ? cs.value.data : null,
      });
    } catch {
      setError("Erreur lors de la génération du rapport.");
    } finally {
      setLoading(false);
    }
  };

  const fmt = (v) => v != null ? new Intl.NumberFormat('fr-FR').format(v) : '—';

  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Rapports de l'Agence</h1>
        <p className="text-gray-500 text-sm">Générez des rapports consolidés pour votre agence</p>
      </div>

      <div className="card">
        <h3 className="font-semibold text-gray-700 mb-4">Paramètres du Rapport</h3>
        <div className="flex items-end gap-4">
          <div>
            <label className="form-label">Date de début</label>
            <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)} className="form-input" />
          </div>
          <div>
            <label className="form-label">Date de fin</label>
            <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)} className="form-input" />
          </div>
          <button onClick={handleGenerate} disabled={loading} className="btn-primary flex items-center gap-2">
            <BarChart2 size={16} />
            {loading ? 'Génération...' : 'Générer le Rapport'}
          </button>
        </div>
        {error && <p className="text-red-500 text-sm mt-3">{error}</p>}
      </div>

      {report && (
        <div className="space-y-4">
          {report.clients && (
            <div className="card">
              <h3 className="font-semibold text-gray-700 mb-3">Statistiques Clients de l'Agence</h3>
              <div className="grid grid-cols-4 gap-4 text-sm">
                <div className="text-center p-3 bg-blue-50 rounded-lg">
                  <p className="text-2xl font-bold text-blue-700">{fmt(report.clients.totalClients)}</p>
                  <p className="text-gray-500 text-xs mt-1">Total clients</p>
                </div>
                <div className="text-center p-3 bg-green-50 rounded-lg">
                  <p className="text-2xl font-bold text-green-700">{fmt(report.clients.totalAccounts)}</p>
                  <p className="text-gray-500 text-xs mt-1">Total comptes</p>
                </div>
                <div className="text-center p-3 bg-orange-50 rounded-lg">
                  <p className="text-2xl font-bold text-orange-700">{fmt(report.clients.totalBalance)} FCFA</p>
                  <p className="text-gray-500 text-xs mt-1">Solde total agence</p>
                </div>
                <div className="text-center p-3 bg-purple-50 rounded-lg">
                  <p className="text-2xl font-bold text-purple-700">{fmt(report.clients.avgAccountsPerClient)}</p>
                  <p className="text-gray-500 text-xs mt-1">Comptes / client moy.</p>
                </div>
              </div>
            </div>
          )}

          {report.loans && (
            <div className="card">
              <h3 className="font-semibold text-gray-700 mb-3">Statistiques Prêts</h3>
              <div className="grid grid-cols-4 gap-4 text-sm">
                <div className="text-center p-3 bg-blue-50 rounded-lg">
                  <p className="text-2xl font-bold text-blue-700">{fmt(report.loans.totalApplications)}</p>
                  <p className="text-gray-500 text-xs mt-1">Total demandes</p>
                </div>
                <div className="text-center p-3 bg-green-50 rounded-lg">
                  <p className="text-2xl font-bold text-green-700">{fmt(report.loans.approvedApplications)}</p>
                  <p className="text-gray-500 text-xs mt-1">Approuvées</p>
                </div>
                <div className="text-center p-3 bg-purple-50 rounded-lg">
                  <p className="text-2xl font-bold text-purple-700">{fmt(report.loans.totalDisbursedAmount)} F</p>
                  <p className="text-gray-500 text-xs mt-1">Montant décaissé</p>
                </div>
                <div className="text-center p-3 bg-orange-50 rounded-lg">
                  <p className="text-2xl font-bold text-orange-700">
                    {report.loans.approvalRate != null ? report.loans.approvalRate.toFixed(1) + '%' : '—'}
                  </p>
                  <p className="text-gray-500 text-xs mt-1">Taux d'approbation</p>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default DirecteurRapportsPage;
