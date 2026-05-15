import React, { useState, useEffect, useCallback } from 'react';
import { Send, Bell, MessageSquare, Mail, Clock, RefreshCw } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import {
  getAllNotifications,
  getNotificationStats,
  sendNotification,
  retryEchecs,
} from '../../api/notificationsApi';

const NOTIFS_AUTO = [
  { titre: 'Rappel 5 jours avant échéance',  description: 'SMS de rappel automatique',          actif: true  },
  { titre: 'Confirmation de paiement',        description: 'SMS après chaque paiement reçu',     actif: true  },
  { titre: 'Alerte retard J+1',               description: 'SMS + Email après 1 jour de retard', actif: true  },
  { titre: 'Newsletter mensuelle',             description: 'Email le 1er de chaque mois',        actif: false },
];

const ONGLETS = ['Toutes', 'SMS', 'Email', 'Alertes'];

const STATUT_LABELS = {
  EN_ATTENTE:      'En attente',
  EN_COURS:        'En cours',
  ENVOYEE:         'Envoyée',
  ECHEC:           'Échec',
  ECHEC_DEFINITIF: 'Échec définitif',
  PROGRAMMEE:      'Programmée',
  LUE:             'Lue',
};

const NotificationsPage = () => {
  const [notifications,  setNotifications]  = useState([]);
  const [stats,          setStats]          = useState(null);
  const [loading,        setLoading]        = useState(true);
  const [ongletActif,    setOngletActif]    = useState('Toutes');
  const [modalOuverte,   setModalOuverte]   = useState(false);
  const [sending,        setSending]        = useState(false);
  const [sendError,      setSendError]      = useState('');
  const [notifsAuto,     setNotifsAuto]     = useState(NOTIFS_AUTO);

  const [notifForm, setNotifForm] = useState({
    clientId: '', email: '', canal: 'EMAIL', type: 'ALERTE_SYSTEME',
    sujet: '', message: '', priorite: '5',
  });

  const load = useCallback(async () => {
    setLoading(true);
    const [notifRes, statsRes] = await Promise.allSettled([
      getAllNotifications(0, 50),
      getNotificationStats(),
    ]);
    if (notifRes.status === 'fulfilled')  setNotifications(notifRes.value.data?.content ?? []);
    if (statsRes.status === 'fulfilled')  setStats(statsRes.value.data);
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleEnvoyer = async (e) => {
    e.preventDefault();
    setSendError('');
    setSending(true);
    try {
      await sendNotification({
        clientId:  notifForm.clientId,
        email:     notifForm.email || undefined,
        canal:     notifForm.canal,
        type:      notifForm.type,
        sujet:     notifForm.sujet,
        message:   notifForm.message,
        priorite:  Number(notifForm.priorite),
      });
      setModalOuverte(false);
      setNotifForm({ clientId: '', email: '', canal: 'EMAIL', type: 'ALERTE_SYSTEME', sujet: '', message: '', priorite: '5' });
      await load();
    } catch (err) {
      setSendError(err.response?.data?.message ?? 'Erreur lors de l\'envoi');
    } finally {
      setSending(false);
    }
  };

  const handleRetry = async () => {
    try {
      await retryEchecs();
      await load();
    } catch {
      // retry silently
    }
  };

  const filtrees = notifications.filter(n => {
    if (ongletActif === 'SMS')    return n.canal === 'SMS';
    if (ongletActif === 'Email')  return n.canal === 'EMAIL';
    if (ongletActif === 'Alertes') return n.type === 'ALERTE_SYSTEME' || n.type === 'RAPPEL_ECHEANCE' || n.type === 'ALERTE_RETARD';
    return true;
  });

  const fmt = (n) => n != null ? String(n) : '—';

  return (
    <div className="p-6 space-y-6">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Notifications</h1>
          <p className="text-gray-500 text-sm">Gérez les notifications SMS, Email et alertes automatiques</p>
        </div>
        <div className="flex gap-2">
          <button onClick={handleRetry} className="btn-secondary flex items-center gap-2 text-sm">
            <RefreshCw size={14} /> Relancer les échecs
          </button>
          <button onClick={() => setModalOuverte(true)} className="btn-primary flex items-center gap-2">
            <Send size={15} /> Nouvelle Notification
          </button>
        </div>
      </div>

      <div className="grid grid-cols-4 gap-4">
        <StatCard title="Total Envoyées" value={loading ? '…' : fmt(stats?.total)}    icon={Send}          iconBg="bg-blue-100"   iconColor="text-blue-600" />
        <StatCard title="En Attente"     value={loading ? '…' : fmt(stats?.enAttente)} icon={Clock}         iconBg="bg-yellow-100" iconColor="text-yellow-600" />
        <StatCard title="Livrées"        value={loading ? '…' : fmt(stats?.envoyees)}  icon={Bell}          iconBg="bg-green-100"  iconColor="text-green-600" />
        <StatCard title="Échecs"         value={loading ? '…' : fmt(stats?.echecs)}    icon={MessageSquare} iconBg="bg-red-100"    iconColor="text-red-500" />
      </div>

      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="font-semibold text-gray-700">Historique des Notifications</h3>
            <p className="text-xs text-gray-400">
              {loading ? 'Chargement…' : `${filtrees.length} notification(s)`}
            </p>
          </div>
          <div className="flex gap-1 bg-gray-100 p-1 rounded-lg">
            {ONGLETS.map(o => (
              <button key={o} onClick={() => setOngletActif(o)}
                className={`px-3 py-1.5 text-xs rounded-md transition-colors font-medium flex items-center gap-1 ${
                  ongletActif === o ? 'bg-white text-gray-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'
                }`}>
                {o === 'SMS'    && <MessageSquare size={11} />}
                {o === 'Email'  && <Mail size={11} />}
                {o === 'Alertes'&& <Bell size={11} />}
                {o}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <p className="text-center text-gray-400 py-10 text-sm">Chargement des notifications…</p>
        ) : filtrees.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-gray-300">
            <Bell size={32} />
            <p className="text-sm mt-2">Aucune notification</p>
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-400 border-b border-gray-100">
                <th className="text-left pb-3">Sujet</th>
                <th className="text-left pb-3">Type</th>
                <th className="text-left pb-3">Canal</th>
                <th className="text-left pb-3">Client</th>
                <th className="text-left pb-3">Date</th>
                <th className="text-left pb-3">Statut</th>
              </tr>
            </thead>
            <tbody>
              {filtrees.map(n => (
                <tr key={n.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="py-3 font-medium text-gray-800 max-w-xs truncate">{n.sujet}</td>
                  <td className="py-3 text-xs text-gray-500">{n.type?.replace(/_/g, ' ')}</td>
                  <td className="py-3">
                    <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full">{n.canal}</span>
                  </td>
                  <td className="py-3 text-xs text-gray-400">{n.clientId || '—'}</td>
                  <td className="py-3 text-xs text-gray-400">
                    {n.createdAt ? new Date(n.createdAt).toLocaleString('fr-FR') : '—'}
                  </td>
                  <td className="py-3">
                    <Badge status={n.statut} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <h3 className="font-semibold text-gray-700 mb-1">Alertes Automatiques</h3>
        <p className="text-xs text-gray-400 mb-4">Déclenchées automatiquement par les événements du système</p>
        <div className="grid grid-cols-2 gap-4">
          {notifsAuto.map((n, i) => (
            <div key={n.titre} className="flex items-center justify-between bg-gray-50 rounded-xl p-4">
              <div>
                <p className="font-medium text-gray-700 text-sm">{n.titre}</p>
                <p className="text-xs text-gray-400">{n.description}</p>
              </div>
              <div onClick={() => setNotifsAuto(prev => prev.map((x, j) => j === i ? { ...x, actif: !x.actif } : x))}
                className={`w-10 h-5 rounded-full cursor-pointer transition-colors flex items-center px-0.5 ${n.actif ? 'bg-green-500' : 'bg-gray-300'}`}>
                <div className={`w-4 h-4 bg-white rounded-full shadow transition-transform ${n.actif ? 'translate-x-5' : 'translate-x-0'}`} />
              </div>
            </div>
          ))}
        </div>
      </div>

      <Modal isOpen={modalOuverte} onClose={() => { setModalOuverte(false); setSendError(''); }}
        title="Envoyer une Notification" subtitle="Créez et envoyez une notification personnalisée">
        <form onSubmit={handleEnvoyer} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">ID Client *</label>
              <input placeholder="Ex: abc-123" value={notifForm.clientId}
                onChange={e => setNotifForm(p => ({...p, clientId: e.target.value}))}
                required className="form-input" />
            </div>
            <div>
              <label className="form-label">Email destinataire</label>
              <input type="email" placeholder="client@email.cm" value={notifForm.email}
                onChange={e => setNotifForm(p => ({...p, email: e.target.value}))}
                className="form-input" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="form-label">Type *</label>
              <select value={notifForm.type} onChange={e => setNotifForm(p => ({...p, type: e.target.value}))} className="form-input" required>
                <option value="ALERTE_SYSTEME">Alerte système</option>
                <option value="RAPPEL_ECHEANCE">Rappel échéance</option>
                <option value="DEMANDE_PRET">Demande prêt</option>
                <option value="APPROBATION_PRET">Approbation prêt</option>
                <option value="REJET_PRET">Rejet prêt</option>
                <option value="CONFIRMATION_REMB">Confirmation remboursement</option>
                <option value="ALERTE_RETARD">Alerte retard</option>
                <option value="DEPOT_EFFECTUE">Dépôt effectué</option>
                <option value="RETRAIT_EFFECTUE">Retrait effectué</option>
                <option value="PROMOTION">Promotion</option>
              </select>
            </div>
            <div>
              <label className="form-label">Canal *</label>
              <select value={notifForm.canal} onChange={e => setNotifForm(p => ({...p, canal: e.target.value}))} className="form-input" required>
                <option value="EMAIL">Email</option>
                <option value="SMS">SMS</option>
                <option value="IN_APP">In-App</option>
                <option value="EMAIL_SMS">Email + SMS</option>
              </select>
            </div>
          </div>
          <div>
            <label className="form-label">Sujet *</label>
            <input placeholder="Ex: Rappel de paiement" value={notifForm.sujet}
              onChange={e => setNotifForm(p => ({...p, sujet: e.target.value}))}
              required className="form-input" />
          </div>
          <div>
            <label className="form-label">Message *</label>
            <textarea rows={3} placeholder="Votre message..." value={notifForm.message}
              onChange={e => setNotifForm(p => ({...p, message: e.target.value}))}
              required className="form-input resize-none" />
          </div>
          {sendError && <p className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{sendError}</p>}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalOuverte(false); setSendError(''); }} className="btn-secondary">Annuler</button>
            <button type="submit" disabled={sending} className="btn-primary flex items-center gap-2 disabled:opacity-60">
              <Send size={14} /> {sending ? 'Envoi…' : 'Envoyer'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default NotificationsPage;
