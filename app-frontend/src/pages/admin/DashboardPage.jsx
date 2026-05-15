import React, { useState, useEffect } from 'react';
import { Users, CreditCard, TrendingUp, AlertCircle } from 'lucide-react';
import {
  BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';
import StatCard from '../../components/common/StatCard';
import { getCurrentUser, getGreeting } from '../../utils/auth';
import { getClientStats } from '../../api/clientsApi';
import { getLoanStats, getPortfolioStats, getPendingApplications } from '../../api/loansApi';
import { getRepaymentStats } from '../../api/repaymentApi';

const PIE_COLORS = ['#2563eb', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6'];

const fmt = (n) => n != null ? Number(n).toLocaleString('fr-FR') : '—';
const fmtPct = (n) => n != null ? `${Number(n).toFixed(1)} %` : '—';

const DashboardPage = () => {
  const user     = getCurrentUser();
  const greeting = getGreeting(user?.prenom || 'Admin');

  const [clientStats,   setClientStats]   = useState(null);
  const [loanStats,     setLoanStats]     = useState(null);
  const [portfolio,     setPortfolio]     = useState(null);
  const [repayStats,    setRepayStats]    = useState(null);
  const [pendingApps,   setPendingApps]   = useState([]);
  const [loading,       setLoading]       = useState(true);

  useEffect(() => {
    const load = async () => {
      const [cs, ls, ps, rs, pa] = await Promise.allSettled([
        getClientStats(),
        getLoanStats(),
        getPortfolioStats(),
        getRepaymentStats(),
        getPendingApplications(0, 5),
      ]);
      if (cs.status === 'fulfilled') setClientStats(cs.value.data);
      if (ls.status === 'fulfilled') setLoanStats(ls.value.data);
      if (ps.status === 'fulfilled') setPortfolio(ps.value.data);
      if (rs.status === 'fulfilled') setRepayStats(rs.value.data);
      if (pa.status === 'fulfilled') setPendingApps(pa.value.data?.content ?? pa.value.data ?? []);
      setLoading(false);
    };
    load();
  }, []);

  const riskData = portfolio ? [
    { name: 'Sain', value: Number(portfolio.totalOutstanding ?? 0) - Number(portfolio.atRisk30Days ?? 0) - Number(portfolio.atRisk90Days ?? 0) },
    { name: '> 30j', value: Number(portfolio.atRisk30Days ?? 0) },
    { name: '> 90j', value: Number(portfolio.atRisk90Days ?? 0) },
  ] : [];

  const clientGrowthData = clientStats ? [
    { label: 'Actifs',    value: clientStats.activeClients    ?? 0 },
    { label: 'En attente',value: clientStats.pendingClients   ?? 0 },
    { label: 'Inactifs',  value: clientStats.inactiveClients  ?? 0 },
    { label: 'Suspendus', value: clientStats.suspendedClients ?? 0 },
  ] : [];

  return (
    <div className="p-6 space-y-6">

      <div>
        <h1 className="text-2xl font-bold text-gray-800">{greeting}</h1>
        <p className="text-gray-500 text-sm">Vue d'ensemble de vos opérations de microfinance</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Clients"
          value={loading ? '...' : fmt(clientStats?.totalClients)}
          icon={Users}
          iconBg="bg-blue-100"
          iconColor="text-blue-600"
          subtitle={clientStats ? `+${clientStats.newClientsThisMonth ?? 0} ce mois` : undefined}
        />
        <StatCard
          title="Prêts Actifs"
          value={loading ? '...' : fmt(loanStats?.activeLoans)}
          icon={CreditCard}
          iconBg="bg-green-100"
          iconColor="text-green-600"
          subtitle={loanStats ? `${fmt(loanStats.disbursedLoans)} décaissés` : undefined}
        />
        <StatCard
          title="Encours (FCFA)"
          value={loading ? '...' : fmt(portfolio?.totalOutstanding)}
          icon={TrendingUp}
          iconBg="bg-purple-100"
          iconColor="text-purple-600"
          subtitle={loanStats ? `Taux approbation: ${fmtPct(loanStats.approvalRate)}` : undefined}
        />
        <StatCard
          title="Retards / Créances"
          value={loading ? '...' : fmt(repayStats?.overdueCount)}
          icon={AlertCircle}
          iconBg="bg-red-100"
          iconColor="text-red-500"
          subtitle={repayStats ? `Taux recouvrement: ${fmtPct(repayStats.repaymentRate)}` : undefined}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">

        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-1">Répartition des Clients</h3>
          <p className="text-xs text-gray-400 mb-4">Distribution par statut</p>
          {clientGrowthData.length > 0 ? (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={clientGrowthData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="value" fill="#2563eb" radius={[4, 4, 0, 0]} name="Clients" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-[200px] text-gray-300 text-sm">
              {loading ? 'Chargement...' : 'Aucune donnée'}
            </div>
          )}
        </div>

        <div className="card flex flex-col">
          <h3 className="font-semibold text-gray-700 mb-1">Risque Portefeuille</h3>
          <p className="text-xs text-gray-400 mb-4">Encours sain vs à risque (30j / 90j)</p>
          {portfolio && Number(portfolio.totalOutstanding ?? 0) > 0 ? (
            <div className="flex items-center gap-4">
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
                    <span className="ml-auto font-medium">{fmt(d.value)}</span>
                  </div>
                ))}
                {portfolio.recoveryRate != null && (
                  <p className="text-gray-400 pt-1">Taux recouvrement : {fmtPct(portfolio.recoveryRate)}</p>
                )}
              </div>
            </div>
          ) : (
            <div className="flex items-center justify-center flex-1 text-gray-300 text-sm">
              {loading ? 'Chargement...' : 'Aucune donnée'}
            </div>
          )}
        </div>

      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">

        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-1">Statistiques Prêts</h3>
          <p className="text-xs text-gray-400 mb-4">Synthèse du cycle de vie des demandes</p>
          {loanStats ? (
            <div className="grid grid-cols-2 gap-3 text-sm">
              {[
                ['Total demandes',   loanStats.totalApplications],
                ['Approuvées',       loanStats.approvedApplications],
                ['Rejetées',         loanStats.rejectedApplications],
                ['Décaissés',        loanStats.disbursedLoans],
                ['Actifs',           loanStats.activeLoans],
                ['Terminés',         loanStats.completedLoans],
                ['En défaut',        loanStats.defaultedLoans],
                ['Taux défaut',      fmtPct(loanStats.defaultRate)],
              ].map(([label, val]) => (
                <div key={label} className="flex justify-between border-b border-gray-50 pb-1">
                  <span className="text-gray-500">{label}</span>
                  <span className="font-semibold text-gray-800">{typeof val === 'string' ? val : fmt(val)}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex items-center justify-center h-24 text-gray-300 text-sm">
              {loading ? 'Chargement...' : 'Aucune donnée'}
            </div>
          )}
        </div>

        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-1">Demandes en Attente</h3>
          <p className="text-xs text-gray-400 mb-4">Dernières demandes à traiter</p>
          {pendingApps.length > 0 ? (
            <div className="space-y-2">
              {pendingApps.slice(0, 5).map((app, i) => (
                <div key={app.id ?? i} className="flex items-center justify-between py-2 border-b border-gray-50 text-sm">
                  <div>
                    <p className="font-medium text-gray-800">{app.clientName ?? `Client #${app.clientId}`}</p>
                    <p className="text-xs text-gray-400">{app.loanType ?? '—'} · {fmt(app.requestedAmount)} FCFA</p>
                  </div>
                  <span className="text-xs bg-yellow-100 text-yellow-700 px-2 py-0.5 rounded-full">En attente</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-8 text-gray-300">
              <CreditCard size={28} />
              <p className="text-xs mt-2">{loading ? 'Chargement...' : 'Aucune demande en attente'}</p>
            </div>
          )}
        </div>

      </div>
    </div>
  );
};

export default DashboardPage;
