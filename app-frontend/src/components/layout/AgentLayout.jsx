import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, Monitor, Users, CreditCard, RefreshCw, User, LogOut, Menu, X
} from 'lucide-react';
import { getCurrentUser, logout } from '../../utils/auth';
import { logoutFromServer } from '../../api/authApi';

const AGENT_NAV = [
  { to: '/agent/dashboard',       icon: LayoutDashboard, label: 'Tableau de bord' },
  { to: '/agent/operations',      icon: Monitor,         label: 'Guichet' },
  { to: '/agent/clients',         icon: Users,           label: 'Mes Clients' },
  { to: '/agent/prets',           icon: CreditCard,      label: 'Demandes Prêt' },
  { to: '/agent/remboursements',  icon: RefreshCw,       label: 'Remboursements' },
  { to: '/agent/profil',          icon: User,            label: 'Mon Profil' },
];

const AgentLayout = ({ children }) => {
  const navigate = useNavigate();
  const user = getCurrentUser();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleLogout = async () => {
    await logoutFromServer();
    logout();
    navigate('/login');
  };

  const closeSidebar = () => setSidebarOpen(false);

  return (
    <div className="flex h-screen overflow-hidden">

      {/* ── OVERLAY MOBILE ─────────────────────────────────────── */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-20 bg-black/50 lg:hidden"
          onClick={closeSidebar}
        />
      )}

      {/* ── SIDEBAR AGENT ───────────────────────────────────────── */}
      <aside
        className={`
          fixed lg:static inset-y-0 left-0 z-30
          w-56 flex-shrink-0 flex flex-col
          transform transition-transform duration-300 ease-in-out
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
        `}
        style={{ backgroundColor: '#0369a1' }}
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

        {/* Navigation */}
        <nav className="flex-1 py-4 overflow-y-auto">
          {AGENT_NAV.map(({ to, icon: Icon, label }) => (
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

        {/* Profil agent en bas */}
        <div className="border-t border-white/10 p-3">
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
      <div className="flex-1 flex flex-col overflow-hidden">
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

export default AgentLayout;
