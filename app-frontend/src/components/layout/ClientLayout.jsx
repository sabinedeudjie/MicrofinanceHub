import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, Plus, Eye, FileText, User, CreditCard, LogOut, AlertTriangle, MessageSquare, Menu, X } from 'lucide-react';
import { getCurrentUser, logout } from '../../utils/auth';
import { logoutFromServer } from '../../api/authApi';

const CLIENT_NAV = [
  { to: '/client/espace',        icon: LayoutDashboard, label: 'Mon Espace' },
  { to: '/client/transactions',  icon: FileText,        label: 'Mes Transactions' },
  { to: '/client/prets',         icon: CreditCard,      label: 'Mes Prêts' },
  { to: '/client/simulateur',    icon: Eye,             label: 'Simulateur' },
  { to: '/client/profil',        icon: User,            label: 'Mon Profil' },
];

const QUICK_ACTIONS = [
  { to: '/client/prets',        icon: Plus,     label: 'Demander un Prêt' },
  { to: '/client/espace',       icon: Eye,      label: 'Consulter mon Solde' },
  { to: '/client/transactions', icon: FileText, label: 'Télécharger Relevé' },
];

const ClientLayout = ({ children }) => {
  const navigate = useNavigate();
  const user = getCurrentUser();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleLogout = async () => {
    await logoutFromServer();
    logout();
    navigate('/login');
  };

  const closeSidebar = () => setSidebarOpen(false);
  const isInactive = user?.status && user.status !== 'ACTIVE';

  return (
    <div className="relative flex h-screen overflow-hidden">

      {/* Overlay pour compte inactif */}
      {isInactive && (
        <div className="absolute inset-0 z-[100] flex items-center justify-center bg-black/40 backdrop-blur-[2px] p-6">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-8 text-center animate-in fade-in zoom-in duration-300">
            <div className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <AlertTriangle size={40} className="text-red-600" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-3">Compte Restreint</h2>
            <p className="text-gray-600 mb-8 leading-relaxed">
              Votre compte client est actuellement marqué comme <span className="font-bold text-red-600">"{user.status}"</span>.
              Vous ne pouvez plus effectuer d'opérations sur la plateforme.
            </p>
            <div className="space-y-3">
              <a
                href="mailto:support@mfh.cm"
                className="flex items-center justify-center gap-2 w-full bg-primary text-white py-3 rounded-xl font-semibold hover:bg-primary/90 transition-colors"
              >
                <MessageSquare size={18} />
                Contacter le Service Client
              </a>
              <button
                onClick={handleLogout}
                className="flex items-center justify-center gap-2 w-full bg-gray-100 text-gray-700 py-3 rounded-xl font-semibold hover:bg-gray-200 transition-colors"
              >
                <LogOut size={18} />
                Se déconnecter
              </button>
            </div>
            <p className="mt-6 text-xs text-gray-400">
              MicroFinanceHub — Sécurité &amp; Conformité
            </p>
          </div>
        </div>
      )}

      {/* ── OVERLAY MOBILE ─────────────────────────────────────── */}
      {sidebarOpen && !isInactive && (
        <div
          className="fixed inset-0 z-20 bg-black/50 lg:hidden"
          onClick={closeSidebar}
        />
      )}

      {/* ── SIDEBAR CLIENT ──────────────────────────────────────── */}
      <aside
        className={`
          fixed lg:static inset-y-0 left-0 z-30
          w-56 flex-shrink-0 flex flex-col
          transform transition-transform duration-300 ease-in-out
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
          ${isInactive ? 'grayscale pointer-events-none opacity-50' : ''}
        `}
        style={{ backgroundColor: '#1e3a8a' }}
      >
        {/* Logo + bouton fermeture mobile */}
        <div className="flex items-center justify-between px-4 py-5 border-b border-white/10">
          <img src="/logo11.PNG" alt="MFH" className="h-10 object-contain" />
          <button
            className="lg:hidden text-white/70 hover:text-white"
            onClick={closeSidebar}
          >
            <X size={20} />
          </button>
        </div>

        {/* Navigation principale */}
        <nav className="py-4">
          {CLIENT_NAV.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              onClick={closeSidebar}
              className={({ isActive }) =>
                `flex items-center gap-3 px-4 py-2.5 mx-2 rounded-lg mb-0.5 text-sm transition-colors ${
                  isActive
                    ? 'bg-white/20 text-white font-medium'
                    : 'text-blue-200 hover:bg-white/10 hover:text-white'
                }`
              }
            >
              <Icon size={17} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        {/* Section actions rapides */}
        <div className="px-3 pb-3">
          <p className="text-blue-400 text-xs font-semibold uppercase tracking-wider px-2 mb-2">
            Actions Rapides
          </p>
          {QUICK_ACTIONS.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={label}
              to={to}
              onClick={closeSidebar}
              className="flex items-center gap-2.5 px-2 py-2 text-blue-200 hover:text-white text-xs rounded-lg hover:bg-white/10 transition-colors"
            >
              <Icon size={14} />
              <span>{label}</span>
            </NavLink>
          ))}
        </div>

        {/* Profil client en bas */}
        <div className="mt-auto border-t border-white/10 p-3">
          <div className="flex items-center gap-2.5 mb-2 px-1">
            <div className="w-8 h-8 bg-white/20 rounded-full flex items-center justify-center">
              <User size={14} className="text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-white text-xs font-medium truncate">{user?.firstName ?? user?.prenom} {user?.lastName ?? user?.nom}</p>
              <p className="text-blue-300 text-xs truncate">{user?.email}</p>
            </div>
          </div>
          <div className="flex items-center justify-between px-1">
            <span className="text-xs bg-white/20 text-white px-2 py-0.5 rounded-full">{user?.role}</span>
            <button onClick={handleLogout} className="text-blue-300 hover:text-white transition-colors" title="Déconnexion">
              <LogOut size={15} />
            </button>
          </div>
        </div>
      </aside>

      {/* ── CONTENU PRINCIPAL ───────────────────────────────────── */}
      <div className={`flex-1 flex flex-col overflow-hidden transition-all ${isInactive ? 'grayscale pointer-events-none opacity-50' : ''}`}>
        {/* Barre de navigation mobile */}
        <header className="lg:hidden flex items-center gap-3 px-4 py-3 border-b border-gray-200 bg-white flex-shrink-0">
          <button
            onClick={() => setSidebarOpen(true)}
            className="text-gray-600 hover:text-gray-900"
          >
            <Menu size={22} />
          </button>
          <img src="/logo1.png" alt="MFH" className="h-7 object-contain" />
        </header>

        <main className="flex-1 overflow-y-auto bg-background">
          {children}
        </main>
      </div>
    </div>
  );
};

export default ClientLayout;
