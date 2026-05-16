import React, { useState, useEffect } from 'react';
import { Users, Activity, DollarSign, AlertTriangle, Building2, UserCheck } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import { getCurrentUser, getGreeting } from '../../utils/auth';
import { getMyClients, getMyClientsStats } from '../../api/clientsApi';
import { getLoanStatsForClients, getPortfolioStatsForClients } from '../../api/loansApi';
import { getRepaymentStatsForClients, getTotalRepaymentsForClients } from '../../api/repaymentApi';
import { getAgentAgencyInternal, getAgencyByIdInternal } from '../../api/agencyApi';

const fmt = (n) => n != null ? Number(n).toLocaleString('fr-FR') : '—';

const AgentDashboardPage = () => {
  const user     = getCurrentUser();
  const greeting = getGreeting(user?.prenom || user?.firstName || 'Agent');

  const [clientStats,  setClientStats]  = useState(null);
  const [loanStats,    setLoanStats]    = useState(null);
  const [portfolio,    setPortfolio]    = useState(null);
  const [totalCollect, setTotalCollect] = useState(null);
  const [clients,      setClients]      = useState([]);
  const [loading,      setLoading]      = useState(true);
  const [agencyInfo,   setAgencyInfo]   = useState(null);

  useEffect(() => {
    if (!user?.id) { setLoading(false); return; }

    const startOfMonth = () => {
      const d = new Date();
      return new Date(d.getFullYear(), d.getMonth(), 1).toISOString();
    };
    const now = () => new Date().toISOString();

    const load = async () => {
      try {
        // 1. Charger d'abord les clients pour avoir leurs IDs réels (portefeuille assigné)
        const clRes = await getMyClients();
        const clientList = clRes.data?.content ?? clRes.data ?? [];
        setClients(clientList);
        
        const clientIds = clientList.map(c => c.id);

        // 2. Charger les statistiques basées sur l'agent lui-même (via ses actions de prêt)
        const [csRes, lsRes, psRes, tcRes] = await Promise.allSettled([
          getMyClientsStats(),
          clientIds.length > 0 ? getLoanStatsForClients(clientIds, null, now()) : Promise.resolve({ data: null }),
          clientIds.length > 0 ? getPortfolioStatsForClients(clientIds) : Promise.resolve({ data: null }),
          clientIds.length > 0 ? getTotalRepaymentsForClients(clientIds, startOfMonth(), now()) : Promise.resolve({ data: null }),
        ]);

        if (csRes.status === 'fulfilled') setClientStats(csRes.value.data);
        if (lsRes.status === 'fulfilled') setLoanStats(lsRes.value.data);
        if (psRes.status === 'fulfilled') setPortfolio(psRes.value.data);
        if (tcRes.status === 'fulfilled') {
          // Si tcRes.data est un objet avec un champ total, on l'extrait
          const val = tcRes.value.data;
          setTotalCollect(typeof val === 'object' ? val.total : val);
        }
      } catch (err) {
        console.error("Erreur lors du chargement des statistiques agent:", err);
      } finally {
        setLoading(false);
      }
    };
    load();

    const loadAgency = async () => {
      try {
        const assignRes = await getAgentAgencyInternal(user.id);
        const assignment = assignRes.data;
        if (!assignment?.agencyId) return;
        const agRes = await getAgencyByIdInternal(assignment.agencyId);
        const ag = agRes.data;
        setAgencyInfo({
          agencyName:   ag.name    ?? assignment.agencyName,
          agencyCode:   ag.code    ?? assignment.agencyCode,
          directorName: ag.directorName ?? null,
        });
      } catch { /* non bloquant */ }
    };
    loadAgency();
  }, [user?.id]);

  const overdueClients = clients.filter(c =>
    c.status === 'OVERDUE' || c.status === 'SUSPENDED'
  );

  return (
    <div className="p-6 space-y-6">

      <div className="rounded-2xl p-5 text-white" style={{ background: 'linear-gradient(135deg, #0369a1, #2563eb)' }}>
        <h1 className="text-xl font-bold">{greeting}</h1>
        <p className="text-blue-200 text-sm">Agent Terrain · {new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' })}</p>
        {agencyInfo && (
          <div className="flex flex-wrap items-center gap-4 mt-3 text-sm text-blue-100">
            <span className="flex items-center gap-1.5">
              <Building2 size={14} />
              {agencyInfo.agencyCode ? `${agencyInfo.agencyName}` : agencyInfo.agencyName}
            </span>
            {agencyInfo.directorName && (
              <span className="flex items-center gap-1.5">
                <UserCheck size={14} /> Directeur : {agencyInfo.directorName}
              </span>
            )}
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Mes Clients"
          value={loading ? '...' : fmt(clientStats?.totalClients ?? clients.length)}
          icon={Users}
          iconBg="bg-green-100"
          iconColor="text-green-600"
          subtitle={clientStats ? `${clientStats.activeClients ?? 0} actifs` : undefined}
        />
        <StatCard
          title="Prêts Actifs"
          value={loading ? '...' : fmt(loanStats?.activeLoans)}
          icon={Activity}
          iconBg="bg-blue-100"
          iconColor="text-blue-600"
          subtitle={loanStats ? `${fmt(loanStats.disbursedLoans)} décaissés` : undefined}
        />
        <StatCard
          title="Total Collecté (FCFA)"
          value={loading ? '...' : fmt(totalCollect)}
          icon={DollarSign}
          iconBg="bg-purple-100"
          iconColor="text-purple-600"
        />
        <StatCard
          title="Retards Portefeuille"
          value={loading ? '...' : fmt(portfolio?.atRisk30Days)}
          icon={AlertTriangle}
          iconBg="bg-red-100"
          iconColor="text-red-500"
          subtitle="Encours > 30j"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">

        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-4">Statistiques des Prêts</h3>
          {loanStats ? (
            <div className="space-y-3 text-sm">
              {[
                ['Total demandes',  loanStats.totalApplications],
                ['Approuvées',      loanStats.approvedApplications],
                ['Actifs',          loanStats.activeLoans],
                ['Terminés',        loanStats.completedLoans],
                ['Taux approbation',`${(loanStats.approvalRate ?? 0).toFixed(1)} %`],
                ['Taux défaut',     `${(loanStats.defaultRate ?? 0).toFixed(1)} %`],
              ].map(([label, val]) => (
                <div key={label} className="flex justify-between border-b border-gray-50 pb-1">
                  <span className="text-gray-500">{label}</span>
                  <span className="font-semibold text-gray-800">{typeof val === 'string' ? val : fmt(val)}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-8 text-gray-300">
              <Activity size={28} />
              <p className="text-sm mt-2">{loading ? 'Chargement...' : 'Aucune donnée prêts'}</p>
            </div>
          )}
        </div>

        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-4">Clients en Situation à Risque</h3>
          {loading ? (
            <div className="flex items-center justify-center py-8 text-gray-300 text-sm">Chargement...</div>
          ) : overdueClients.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-10 text-gray-300">
              <AlertTriangle size={28} />
              <p className="text-sm mt-2">Aucun client en retard</p>
            </div>
          ) : (
            <div className="space-y-2">
              {overdueClients.slice(0, 5).map(c => (
                <div key={c.id} className="bg-red-50 border border-red-100 rounded-xl p-3">
                  <p className="font-medium text-gray-800 text-sm">{c.firstName} {c.lastName}</p>
                  <p className="text-xs text-gray-400">{c.email}</p>
                  <span className="text-xs text-red-600 font-medium">{c.status}</span>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>
    </div>
  );
};

export default AgentDashboardPage;
