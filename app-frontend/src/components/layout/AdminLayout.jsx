import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, Users, CreditCard, Wallet,
  ArrowLeftRight, RefreshCw, BarChart2, Bell, UserCheck, Settings, LogOut, Building2, UserCog, ClipboardCheck, Menu, X
} from 'lucide-react';
import { getCurrentUser, logout } from '../../utils/auth';
import { logoutFromServer } from '../../api/authApi';

const ADMIN_NAV = [
  { to: '/admin/dashboard',       icon: LayoutDashboard, label: 'Tableau de bord' },
  { to: '/admin/clients',         icon: Users,           label: 'Clients' },
  { to: '/admin/prets',           icon: CreditCard,      label: 'Prêts' },
  { to: '/admin/comptes',         icon: Wallet,          label: 'Comptes' },
  { to: '/admin/validations',     icon: ClipboardCheck,  label: 'Validations' },
  { to: '/admin/transactions',    icon: ArrowLeftRight,  label: 'Transactions' },
  { to: '/admin/remboursements',  icon: RefreshCw,       label: 'Remboursements' },
  { to: '/admin/rapports',        icon: BarChart2,       label: 'Rapports' },
  { to: '/admin/notifications',   icon: Bell,            label: 'Notifications' },
  { to: '/admin/agents',          icon: UserCheck,       label: 'Agents' },
  { to: '/admin/directeurs',      icon: UserCog,         label: 'Directeurs' },
  { to: '/admin/agences',         icon: Building2,       label: 'Agences' },
  { to: '/admin/parametres',      icon: Settings,        label: 'Paramètres' },
];

const AdminLayout = ({ children }) => {
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

      {/* ── SIDEBAR ─────────────────────────────────────────────── */}
      <aside
        className={`
          fixed lg:static inset-y-0 left-0 z-30
          w-56 flex-shrink-0 flex flex-col
          transform transition-transform duration-300 ease-in-out
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
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

        {/* Liens de navigation */}
        <nav className="flex-1 py-4 overflow-y-auto">
          {ADMIN_NAV.map(({ to, icon: Icon, label, badge }) => (
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
              <span className="flex-1">{label}</span>
              {badge && (
                <span className="bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                  {badge}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        {/* Profil et déconnexion */}
        <div className="border-t border-white/10 p-3">
          <div className="flex items-center gap-2.5 mb-2 px-1">
            <div className="w-8 h-8 bg-white/20 rounded-full flex items-center justify-center">
              <Users size={14} className="text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-white text-xs font-medium truncate">{user?.prenom} {user?.nom}</p>
              <p className="text-blue-300 text-xs truncate">{user?.email}</p>
            </div>
          </div>
          <div className="flex items-center justify-between px-1">
            <span className="text-xs bg-white/20 text-white px-2 py-0.5 rounded-full">
              {user?.role}
            </span>
            <button
              onClick={handleLogout}
              className="text-blue-300 hover:text-white transition-colors"
              title="Déconnexion"
            >
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

export default AdminLayout;
