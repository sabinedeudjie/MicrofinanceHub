import React, { useState, useEffect } from 'react';
import { DollarSign, TrendingUp, Users, AlertTriangle, BarChart2 } from 'lucide-react';
import {
  BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';
import StatCard from '../../components/common/StatCard';
import { getLoanStats, getPortfolioStats } from '../../api/loansApi';
import { getRepaymentStats } from '../../api/repaymentApi';
import { getClientStats } from '../../api/clientsApi';

const PIE_COLORS = ['#2563eb', '#22c55e', '#f59e0b', '#ef4444'];

const fmt = (n) => n != null ? Number(n).toLocaleString('fr-FR') : '—';
const fmtPct = (n) => n != null ? `${Number(n).toFixed(1)} %` : '—';

const RapportsPage = () => {
  const [loanStats,   setLoanStats]   = useState(null);
  const [portfolio,   setPortfolio]   = useState(null);
  const [repayStats,  setRepayStats]  = useState(null);
  const [clientStats, setClientStats] = useState(null);
  const [loading,     setLoading]     = useState(true);

  useEffect(() => {
    Promise.allSettled([
      getLoanStats(),
      getPortfolioStats(),
      getRepaymentStats(),
      getClientStats(),
    ]).then(([ls, ps, rs, cs]) => {
      if (ls.status === 'fulfilled') setLoanStats(ls.value.data);
      if (ps.status === 'fulfilled') setPortfolio(ps.value.data);
      if (rs.status === 'fulfilled') setRepayStats(rs.value.data);
      if (cs.status === 'fulfilled') setClientStats(cs.value.data);
      setLoading(false);
    });
  }, []);

  const clientDistData = clientStats ? [
    { label: 'Actifs',     value: clientStats.activeClients    ?? 0 },
    { label: 'En attente', value: clientStats.pendingClients   ?? 0 },
    { label: 'Inactifs',   value: clientStats.inactiveClients  ?? 0 },
    { label: 'Suspendus',  value: clientStats.suspendedClients ?? 0 },
  ] : [];

  const riskData = portfolio ? [
    { name: 'Sain',   value: Math.max(0, Number(portfolio.totalOutstanding ?? 0) - Number(portfolio.atRisk30Days ?? 0) - Number(portfolio.atRisk90Days ?? 0)) },
    { name: '> 30j',  value: Number(portfolio.atRisk30Days  ?? 0) },
    { name: '> 90j',  value: Number(portfolio.atRisk90Days  ?? 0) },
  ] : [];

  const loanLifecycleData = loanStats ? [
    { label: 'Demandés',  value: loanStats.totalApplications   ?? 0 },
    { label: 'Approuvés', value: loanStats.approvedApplications ?? 0 },
    { label: 'Décaissés', value: loanStats.disbursedLoans       ?? 0 },
    { label: 'Actifs',    value: loanStats.activeLoans          ?? 0 },
    { label: 'Terminés',  value: loanStats.completedLoans       ?? 0 },
    { label: 'Défaut',    value: loanStats.defaultedLoans       ?? 0 },
  ] : [];

  return (
    <div className="p-6 space-y-6">

      <div>
        <h1 className="text-2xl font-bold text-gray-800">Rapports & Analytics</h1>
        <p className="text-gray-500 text-sm">Analyses financières et tableaux de bord</p>
      </div>

      <div className="grid grid-cols-4 gap-4">
        <StatCard
          title="Encours Portfolio"
          value={loading ? '...' : fmt(portfolio?.totalOutstanding)}
          icon={DollarSign}
          iconBg="bg-green-100"
          iconColor="text-green-600"
          subtitle={portfolio?.recoveryRate != null ? `Recouvrement : ${fmtPct(portfolio.recoveryRate)}` : undefined}
        />
        <StatCard
          title="Taux de Remboursement"
          value={loading ? '...' : fmtPct(repayStats?.repaymentRate)}
          icon={TrendingUp}
          iconBg="bg-blue-100"
          iconColor="text-blue-600"
          subtitle={repayStats ? `${fmt(repayStats.overdueCount)} en retard` : undefined}
        />
        <StatCard
          title="Total Clients"
          value={loading ? '...' : fmt(clientStats?.totalClients)}
          icon={Users}
          iconBg="bg-purple-100"
          iconColor="text-purple-600"
          subtitle={clientStats ? `+${clientStats.newClientsThisMonth ?? 0} ce mois` : undefined}
        />
        <StatCard
          title="Taux de Défaut"
          value={loading ? '...' : fmtPct(loanStats?.defaultRate)}
          icon={AlertTriangle}
          iconBg="bg-orange-100"
          iconColor="text-orange-500"
          subtitle={loanStats ? `${fmt(loanStats.defaultedLoans)} prêts` : undefined}
        />
      </div>

      <div className="grid grid-cols-2 gap-4">

        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-1">Cycle de vie des Prêts</h3>
          <p className="text-xs text-gray-400 mb-4">Répartition par étape du processus</p>
          {loanLifecycleData.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={loanLifecycleData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="label" tick={{ fontSize: 10 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="value" fill="#2563eb" radius={[4, 4, 0, 0]} name="Prêts" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex flex-col items-center justify-center h-[220px] text-gray-300">
              <BarChart2 size={32} />
              <p className="text-sm mt-2">{loading ? 'Chargement...' : 'Aucune donnée'}</p>
            </div>
          )}
        </div>

        <div className="card flex flex-col">
          <h3 className="font-semibold text-gray-700 mb-1">Risque Portefeuille</h3>
          <p className="text-xs text-gray-400 mb-4">Encours sain vs à risque</p>
          {portfolio && Number(portfolio.totalOutstanding ?? 0) > 0 ? (
            <div className="flex items-center gap-4 flex-1">
              <ResponsiveContainer width="55%" height={180}>
                <PieChart>
                  <Pie data={riskData} cx="50%" cy="50%" outerRadius={75} dataKey="value" stroke="none">
                    {riskData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                  </Pie>
                  <Tooltip formatter={(v) => fmt(v) + ' FCFA'} />
                </PieChart>
              </ResponsiveContainer>
              <div className="flex-1 space-y-2 text-xs">
                {riskData.map((d, i) => (
                  <div key={d.name} className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-sm" style={{ background: PIE_COLORS[i] }} />
                    <span className="text-gray-600">{d.name}</span>
                    <span className="ml-auto font-medium">{fmt(d.value)} FCFA</span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center flex-1 text-gray-300">
              <BarChart2 size={28} />
              <p className="text-sm mt-2">{loading ? 'Chargement...' : 'Aucune donnée'}</p>
            </div>
          )}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">

        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-1">Répartition des Clients</h3>
          <p className="text-xs text-gray-400 mb-4">Distribution par statut</p>
          {clientDistData.length > 0 ? (
            <ResponsiveContainer width="100%" height={180}>
              <BarChart data={clientDistData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="value" fill="#8b5cf6" radius={[4, 4, 0, 0]} name="Clients" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex flex-col items-center justify-center h-[180px] text-gray-300">
              <BarChart2 size={28} />
              <p className="text-sm mt-2">{loading ? 'Chargement...' : 'Aucune donnée'}</p>
            </div>
          )}
        </div>

        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-4">Rapport de Synthèse</h3>
          <p className="text-xs text-gray-400 mb-4">Indicateurs clés de performance</p>
          <div className="grid grid-cols-2 gap-3 text-sm">
            {[
              ['Portfolio total',         fmt(portfolio?.totalOutstanding) + ' FCFA'],
              ['Prêts actifs',            fmt(loanStats?.activeLoans)],
              ['Taux approbation',        fmtPct(loanStats?.approvalRate)],
              ['Taux recouvrement',       fmtPct(portfolio?.recoveryRate)],
              ['Remboursements en retard',fmt(repayStats?.overdueCount)],
              ['Nouveaux clients / mois', fmt(clientStats?.newClientsThisMonth)],
            ].map(([label, val]) => (
              <div key={label} className="bg-gray-50 rounded-xl p-3">
                <p className="text-xs text-gray-400">{label}</p>
                <p className="text-lg font-bold text-gray-800 mt-0.5">{loading ? '...' : val}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default RapportsPage;
