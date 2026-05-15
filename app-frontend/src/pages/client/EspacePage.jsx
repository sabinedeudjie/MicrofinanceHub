import React, { useState, useEffect } from 'react';
import { Wallet, CreditCard, TrendingDown, TrendingUp, CheckCircle, Building2, UserCheck } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import { getCurrentUser, getGreeting } from '../../utils/auth';
import { getComptesByClient, getSoldeTotalClient } from '../../api/comptesApi';
import { getTransactions, compterTransactions } from '../../api/transactionsApi';
import { getClientLoans } from '../../api/loansApi';
import { getMyClientProfile } from '../../api/clientsApi';
import { getAgencyByIdInternal } from '../../api/agencyApi';
import { getPublicUserByEmail } from '../../api/authApi';
import { formatMontant, formatDate, formatTypeTransaction, isCredit } from '../../utils/formatters';

const fmt = (n) => n != null ? Number(n).toLocaleString('fr-FR') : '—';

const STATUS_COLORS = {
  ACTIVE: 'bg-green-100 text-green-700',
  DISBURSED: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-gray-100 text-gray-600',
  PENDING: 'bg-yellow-100 text-yellow-700',
  DEFAULTED: 'bg-red-100 text-red-600',
};

const EspacePage = () => {
  const user     = getCurrentUser();
  const greeting = getGreeting(user?.prenom || user?.firstName || 'Client');

  const [comptes,       setComptes]       = useState([]);
  const [transactions,  setTransactions]  = useState([]);
  const [prets,         setPrets]         = useState([]);
  const [soldeTotal,    setSoldeTotal]    = useState(null);
  const [nbTransactions, setNbTransactions] = useState(null);
  const [loading,       setLoading]       = useState(true);
  const [agencyInfo,    setAgencyInfo]    = useState(null);
  const [agentInfo,     setAgentInfo]     = useState(null);

  useEffect(() => {
    if (!user?.clientId && !user?.id) { setLoading(false); return; }
    const clientId = user.clientId ?? user.id;

    const fetchData = async () => {
      const [comptesRes, soldeRes, pretsRes] = await Promise.allSettled([
        getComptesByClient(clientId, 0, 10),
        getSoldeTotalClient(clientId),
        getClientLoans(clientId),
      ]);

      const comptesList = comptesRes.status === 'fulfilled'
        ? (comptesRes.value.data?.data?.content ?? comptesRes.value.data?.content ?? [])
        : [];
      setComptes(comptesList);
      if (soldeRes.status === 'fulfilled') setSoldeTotal(soldeRes.value.data?.data ?? soldeRes.value.data ?? 0);
      if (pretsRes.status === 'fulfilled') setPrets(pretsRes.value.data ?? []);

      const premierCompte = comptesList.find(c => c.statut === 'ACTIF') ?? comptesList[0];
      if (premierCompte) {
        const [txRes, countRes] = await Promise.allSettled([
          getTransactions(premierCompte.id, 0, 3),
          compterTransactions(premierCompte.id),
        ]);
        if (txRes.status === 'fulfilled')
          setTransactions(txRes.value.data?.data?.content ?? txRes.value.data?.content ?? []);
        if (countRes.status === 'fulfilled')
          setNbTransactions(countRes.value.data?.data ?? countRes.value.data ?? 0);
      }
      setLoading(false);
    };
    fetchData();

    const fetchAffililiation = async () => {
      try {
        const clientRes = await getMyClientProfile();
        const profile = clientRes.data;
        if (profile?.agencyId) {
          const agRes = await getAgencyByIdInternal(profile.agencyId);
          setAgencyInfo({ name: agRes.data.name, code: agRes.data.code });
        }
        if (profile?.createdBy) {
          const agentRes = await getPublicUserByEmail(profile.createdBy);
          const ag = agentRes.data;
          setAgentInfo({ name: `${ag.firstName} ${ag.lastName}`, email: ag.email });
        }
      } catch { /* non bloquant */ }
    };
    fetchAffililiation();
  }, [user?.clientId, user?.id]);

  const totalEmprunte  = prets.reduce((s, p) => s + Number(p.amount ?? 0), 0);
  const totalRembourse = prets.reduce((s, p) => s + (Number(p.amount ?? 0) - Number(p.remainingBalance ?? 0)), 0);

  return (
    <div className="space-y-5">

      <div className="p-6 text-white" style={{ background: 'linear-gradient(135deg, #1e3a8a, #2563eb)' }}>
        <h1 className="text-2xl font-bold">{greeting}</h1>
        <p className="text-blue-200 text-sm mt-0.5">Gérez vos finances en toute simplicité</p>
        <div className="flex flex-wrap items-center gap-4 mt-3 text-sm text-blue-100">
          <span className="flex items-center gap-1.5">
            <CreditCard size={14} /> {user?.email}
          </span>
          {agencyInfo && (
            <span className="flex items-center gap-1.5">
              <Building2 size={14} />
              {agencyInfo.code ? `${agencyInfo.name} (${agencyInfo.code})` : agencyInfo.name}
            </span>
          )}
          {agentInfo && (
            <span className="flex items-center gap-1.5">
              <UserCheck size={14} /> Agent : {agentInfo.name}
            </span>
          )}
        </div>
      </div>

      <div className="px-6 space-y-5">

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            title="Solde Total"
            value={loading ? '…' : soldeTotal != null ? formatMontant(soldeTotal) + ' FCFA' : '— FCFA'}
            icon={Wallet} iconBg="bg-green-100" iconColor="text-green-600"
          />
          <StatCard
            title="Comptes Actifs"
            value={loading ? '…' : String(comptes.filter(c => c.statut === 'ACTIF').length)}
            icon={CreditCard} iconBg="bg-blue-100" iconColor="text-blue-600"
          />
          <StatCard
            title="Total Emprunté"
            value={loading ? '…' : fmt(totalEmprunte) + ' FCFA'}
            icon={TrendingDown} iconBg="bg-purple-100" iconColor="text-purple-600"
          />
          <StatCard
            title="Déjà Remboursé"
            value={loading ? '…' : fmt(totalRembourse) + ' FCFA'}
            icon={CheckCircle} iconBg="bg-orange-100" iconColor="text-orange-600"
          />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-5 gap-4">

          <div className="lg:col-span-2 card">
            <h3 className="font-semibold text-gray-700 mb-1">Transactions Récentes</h3>
            <p className="text-xs text-gray-400 mb-4">
              {nbTransactions != null ? `${nbTransactions} opération(s) au total` : 'Vos dernières opérations'}
            </p>
            {loading ? (
              <p className="text-center text-gray-400 text-sm py-4">Chargement…</p>
            ) : transactions.length === 0 ? (
              <p className="text-center text-gray-400 text-sm py-4">Aucune transaction</p>
            ) : (
              <div className="space-y-3">
                {transactions.map(t => {
                  const credit = isCredit(t.typeTransaction);
                  return (
                    <div key={t.id} className="flex items-center justify-between py-2 border-b border-gray-50">
                      <div className="flex items-center gap-3">
                        <div className={`w-8 h-8 rounded-full flex items-center justify-center ${credit ? 'bg-green-100' : 'bg-red-100'}`}>
                          {credit
                            ? <TrendingDown size={14} className="text-green-600" />
                            : <TrendingUp   size={14} className="text-red-500" />}
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-700">{formatTypeTransaction(t.typeTransaction)}</p>
                          <p className="text-xs text-gray-400">{formatDate(t.dateTransaction)}</p>
                        </div>
                      </div>
                      <span className={`font-semibold text-sm ${credit ? 'text-green-600' : 'text-red-500'}`}>
                        {credit ? '+' : '-'}{formatMontant(t.montant)} FCFA
                      </span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          <div className="lg:col-span-3 card">
            <h3 className="font-semibold text-gray-700 mb-1">Mes Prêts en Cours</h3>
            <p className="text-xs text-gray-400 mb-4">{prets.length} prêt(s) enregistré(s)</p>
            {loading ? (
              <p className="text-center text-gray-400 text-sm py-4">Chargement…</p>
            ) : prets.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-8 text-gray-300">
                <CreditCard size={28} />
                <p className="text-sm mt-2">Aucun prêt enregistré</p>
              </div>
            ) : (
              <div className="space-y-3">
                {prets.filter(p => p.status === 'ACTIVE' || p.status === 'DISBURSED').slice(0, 3).map(p => (
                  <div key={p.id} className="bg-gray-50 rounded-xl p-4">
                    <div className="flex justify-between items-start mb-2">
                      <div>
                        <p className="font-semibold text-gray-800">{fmt(p.amount)} FCFA</p>
                        <p className="text-xs text-gray-400">{p.loanNumber} · {p.termMonths} mois</p>
                      </div>
                      <span className={`text-xs px-2 py-0.5 rounded-full ${STATUS_COLORS[p.status] ?? 'bg-gray-100 text-gray-600'}`}>
                        {p.status}
                      </span>
                    </div>
                    <div className="flex justify-between text-xs text-gray-500">
                      <span>Mensualité : {fmt(p.monthlyPayment)} FCFA</span>
                      <span>Restant : {fmt(p.remainingBalance)} FCFA</span>
                    </div>
                    {p.nextPaymentDate && (
                      <p className="text-xs text-blue-600 mt-1">
                        Prochaine échéance : {new Date(p.nextPaymentDate).toLocaleDateString('fr-FR')}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default EspacePage;
