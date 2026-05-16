import React, { useState, useEffect } from 'react';
import { Users, CreditCard, TrendingUp, UserCheck } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import { getCurrentUser, getGreeting } from '../../utils/auth';
import { getMyAgency, getMyAgencyClients } from '../../api/agencyApi';
import { getPortfolioStatsForClients } from '../../api/loansApi';

const DirecteurDashboardPage = () => {
  const user     = getCurrentUser();
  const greeting = getGreeting(user?.prenom || user?.firstName || 'Directeur');

  const [agency, setAgency]               = useState(null);
  const [clients, setClients]             = useState([]);
  const [portfolioStats, setPortfolioStats] = useState(null);
  const [loading, setLoading]             = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const agencyRes = await getMyAgency();
        const ag = agencyRes.data;
        setAgency(ag);

        const clientsRes = await getMyAgencyClients().catch(() => ({ data: { clients: [] } }));
        const agencyClients = (clientsRes.data?.clients ?? []).map(c => ({
          id:        c.clientId,
          status:    c.clientStatus,
          createdAt: c.clientCreatedAt,
        }));
        setClients(agencyClients);

        if (agencyClients.length > 0) {
          const clientIds = agencyClients.map(c => c.id);
          const psRes = await getPortfolioStatsForClients(clientIds).catch(() => null);
          if (psRes) setPortfolioStats(psRes.data);
        }
      } catch {
        // silently degrade
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const activeClients  = clients.filter(c => c.status?.toString() === 'ACTIVE').length;
  const now            = new Date();
  const newThisMonth   = clients.filter(c => {
    if (!c.createdAt) return false;
    const d = new Date(c.createdAt);
    return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
  }).length;

  const fmt = (v) => v != null ? new Intl.NumberFormat('fr-FR').format(v) : '—';

  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">{greeting}</h1>
        <p className="text-gray-500 text-sm">
          {agency ? `${agency.name}` : 'Vue d\'ensemble de votre agence'}
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Clients de l'agence"
          value={loading ? '…' : fmt(clients.length)}
          icon={Users}
          iconBg="bg-blue-100"
          iconColor="text-blue-600"
        />
        <StatCard
          title="Clients actifs"
          value={loading ? '…' : fmt(activeClients)}
          icon={UserCheck}
          iconBg="bg-green-100"
          iconColor="text-green-600"
        />
        <StatCard
          title="Encours (FCFA)"
          value={loading ? '…' : fmt(portfolioStats?.totalOutstanding)}
          icon={TrendingUp}
          iconBg="bg-purple-100"
          iconColor="text-purple-600"
        />
        <StatCard
          title="Nouveaux ce mois"
          value={loading ? '…' : fmt(newThisMonth)}
          icon={CreditCard}
          iconBg="bg-orange-100"
          iconColor="text-orange-500"
        />
      </div>

      {portfolioStats && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="card">
            <p className="text-xs text-gray-400 mb-1">Portefeuille à risque 30j</p>
            <p className="text-2xl font-bold text-red-600">{fmt(portfolioStats.atRisk30Days)} FCFA</p>
          </div>
          <div className="card">
            <p className="text-xs text-gray-400 mb-1">Portefeuille à risque 90j</p>
            <p className="text-2xl font-bold text-red-700">{fmt(portfolioStats.atRisk90Days)} FCFA</p>
          </div>
          <div className="card">
            <p className="text-xs text-gray-400 mb-1">Taux de recouvrement</p>
            <p className="text-2xl font-bold text-green-600">
              {portfolioStats.recoveryRate != null ? portfolioStats.recoveryRate.toFixed(1) + ' %' : '—'}
            </p>
          </div>
        </div>
      )}

      {agency && (
        <div className="card">
          <h3 className="font-semibold text-gray-700 mb-3">Mon Agence</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 text-sm">
            <div>
              <p className="text-gray-400 text-xs">Code</p>
              <p className="font-medium text-gray-800">{agency.code}</p>
            </div>
            <div>
              <p className="text-gray-400 text-xs">Nom</p>
              <p className="font-medium text-gray-800">{agency.name}</p>
            </div>
            <div>
              <p className="text-gray-400 text-xs">Ville</p>
              <p className="font-medium text-gray-800">{agency.city || '—'}</p>
            </div>
            <div>
              <p className="text-gray-400 text-xs">Statut</p>
              <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${
                agency.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
              }`}>{agency.status}</span>
            </div>
            <div>
              <p className="text-gray-400 text-xs">Agents</p>
              <p className="font-medium text-gray-800">{agency.agentsCount ?? '—'}</p>
            </div>
            <div>
              <p className="text-gray-400 text-xs">Adresse</p>
              <p className="font-medium text-gray-800">{agency.address || '—'}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DirecteurDashboardPage;
