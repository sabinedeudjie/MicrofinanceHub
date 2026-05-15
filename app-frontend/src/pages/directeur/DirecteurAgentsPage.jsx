import React, { useState, useEffect } from 'react';
import { UserCheck, Users, UserMinus, AlertTriangle, ToggleLeft, ToggleRight } from 'lucide-react';
import Badge from '../../components/common/Badge';
import StatCard from '../../components/common/StatCard';
import Modal from '../../components/common/Modal';
import { getMyAgencyAgents, unassignMyAgent, toggleMyAgentStatus } from '../../api/agencyApi';

const DirecteurAgentsPage = () => {
  const [agents, setAgents]           = useState([]);
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState('');
  const [modalUnassign, setModalUnassign] = useState(false);
  const [agentToUnassign, setAgentToUnassign] = useState(null);
  const [unassigning, setUnassigning] = useState(false);
  const [unassignError, setUnassignError] = useState('');

  const loadAgents = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getMyAgencyAgents();
      setAgents(res.data || []);
    } catch {
      setError('Impossible de charger les agents de votre agence.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadAgents(); }, []);

  const openUnassign = (agent) => {
    setAgentToUnassign(agent);
    setUnassignError('');
    setModalUnassign(true);
  };

  const handleUnassign = async () => {
    if (!agentToUnassign) return;
    setUnassigning(true);
    setUnassignError('');
    try {
      await unassignMyAgent(agentToUnassign.agentId);
      await loadAgents();
      setModalUnassign(false);
      setAgentToUnassign(null);
    } catch (err) {
      setUnassignError(err.response?.data?.message ?? 'Erreur lors du désassignement.');
    } finally {
      setUnassigning(false);
    }
  };

  const handleToggleStatus = async (agent) => {
    try {
      await toggleMyAgentStatus(agent.agentId);
      await loadAgents();
    } catch (err) {
      alert(err.response?.data?.message ?? 'Erreur lors du changement de statut.');
    }
  };

  const active   = agents.filter(a => a.active);
  const inactive = agents.filter(a => !a.active);

  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Agents de mon Agence</h1>
        <p className="text-gray-500 text-sm">Agents assignés sous votre supervision</p>
      </div>

      <div className="grid grid-cols-3 gap-4">
        <StatCard title="Total Agents"    value={String(agents.length)}  icon={UserCheck} iconBg="bg-blue-100"  iconColor="text-blue-600" />
        <StatCard title="Actifs"          value={String(active.length)}  icon={Users}     iconBg="bg-green-100" iconColor="text-green-600" />
        <StatCard title="Inactifs"        value={String(inactive.length)}icon={Users}     iconBg="bg-red-100"   iconColor="text-red-500" />
      </div>

      <div className="card">
        <h3 className="font-semibold text-gray-700 mb-4">Liste des Agents</h3>

        {error && <p className="text-red-500 text-sm mb-4 bg-red-50 px-3 py-2 rounded-lg">{error}</p>}

        <table className="w-full text-sm">
          <thead>
            <tr className="text-xs text-gray-400 border-b border-gray-100">
              <th className="text-left pb-3">Nom</th>
              <th className="text-left pb-3">Email</th>
              <th className="text-left pb-3">Statut</th>
              <th className="text-left pb-3">Assigné le</th>
              <th className="text-left pb-3">Référence</th>
              <th className="text-left pb-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className="py-8 text-center text-gray-400">Chargement...</td></tr>
            ) : agents.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-12 text-center text-gray-300">
                  <UserCheck size={32} className="mx-auto mb-2" />
                  <p className="text-sm">Aucun agent assigné à votre agence</p>
                </td>
              </tr>
            ) : (
              agents.map(a => (
                <tr key={a.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="py-3 font-medium text-gray-800">{a.agentName}</td>
                  <td className="py-3 text-gray-500">{a.agentEmail}</td>
                  <td className="py-3"><Badge status={a.active ? 'actif' : 'inactif'} /></td>
                  <td className="py-3 text-xs text-gray-400">
                    {a.assignedAt ? new Date(a.assignedAt).toLocaleDateString('fr-FR') : '—'}
                  </td>
                  <td className="py-3 text-xs text-gray-400 font-mono">{a.reference || '—'}</td>
                  <td className="py-3">
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => handleToggleStatus(a)}
                        title={a.active ? 'Rendre inactif' : 'Rendre actif'}
                        className={`p-1.5 rounded-lg ${a.active ? 'hover:bg-orange-50 text-gray-400 hover:text-orange-500' : 'hover:bg-green-50 text-gray-400 hover:text-green-600'}`}>
                        {a.active ? <ToggleRight size={15} /> : <ToggleLeft size={15} />}
                      </button>
                      <button
                        onClick={() => openUnassign(a)}
                        className="p-1.5 hover:bg-red-50 text-gray-400 hover:text-red-500 rounded-lg"
                        title="Retirer de l'agence">
                        <UserMinus size={15} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <Modal isOpen={modalUnassign} onClose={() => setModalUnassign(false)}
        title="Retirer l'Agent de l'Agence"
        subtitle={agentToUnassign ? agentToUnassign.agentName : ''}>
        <div className="space-y-4">
          <div className="bg-orange-50 border border-orange-200 rounded-xl p-4 text-sm text-orange-700 flex gap-3">
            <AlertTriangle size={18} className="flex-shrink-0 mt-0.5" />
            <p>L'agent sera retiré de votre agence. Cette action peut être annulée par l'administrateur.</p>
          </div>
          {unassignError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{unassignError}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={() => setModalUnassign(false)} className="btn-secondary">Annuler</button>
            <button onClick={handleUnassign} disabled={unassigning}
              className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg text-sm font-medium disabled:opacity-60 flex items-center gap-2">
              <UserMinus size={14} /> {unassigning ? 'Retrait...' : 'Confirmer le retrait'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default DirecteurAgentsPage;
