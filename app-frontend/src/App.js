import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { getCurrentUser, getToken } from './utils/auth';

// ── Layouts ──────────────────────────────────────────────────────────────────
import AdminLayout    from './components/layout/AdminLayout';
import AgentLayout    from './components/layout/AgentLayout';
import ClientLayout   from './components/layout/ClientLayout';
import DirecteurLayout from './components/layout/DirecteurLayout';

// ── Pages Auth ───────────────────────────────────────────────────────────────
import LoginPage          from './pages/auth/LoginPage';
import RegisterPage       from './pages/auth/RegisterPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ResetPasswordPage  from './pages/auth/ResetPasswordPage';

// ── Pages Admin ──────────────────────────────────────────────────────────────
import DashboardPage          from './pages/admin/DashboardPage';
import ClientsPage            from './pages/admin/ClientsPage';
import ValidationsPage        from './pages/admin/ValidationsPage';
import ClientDetailPage       from './pages/admin/ClientDetailPage';
import PretsPage              from './pages/admin/PretsPage';
import PretDetailPage         from './pages/admin/PretDetailPage';
import ComptesPage            from './pages/admin/ComptesPage';
import TransactionsAdminPage  from './pages/admin/TransactionsAdminPage';
import RemboursementsPage     from './pages/admin/RemboursementsPage';
import RapportsPage           from './pages/admin/RapportsPage';
import NotificationsPage      from './pages/admin/NotificationsPage';
import AgentsPage             from './pages/admin/AgentsPage';
import DirecteursPage         from './pages/admin/DirecteursPage';
import ParametresPage         from './pages/admin/ParametresPage';
import AgencesPage            from './pages/admin/AgencesPage';

// ── Pages Agent ──────────────────────────────────────────────────────────────
import AgentDashboardPage      from './pages/agent/AgentDashboardPage';
import OperationsPage          from './pages/agent/OperationsPage';
import AgentClientsPage        from './pages/agent/AgentClientsPage';
import AgentClientDetailPage   from './pages/agent/AgentClientDetailPage';
import AgentPretsPage          from './pages/agent/AgentPretsPage';
import AgentRemboursementsPage from './pages/agent/AgentRemboursementsPage';
import AgentProfilPage         from './pages/agent/AgentProfilPage';

// ── Pages Client ─────────────────────────────────────────────────────────────
import EspacePage       from './pages/client/EspacePage';
import TransactionsPage from './pages/client/TransactionsPage';
import PretsClientPage  from './pages/client/PretsClientPage';
import SimulateurPage   from './pages/client/SimulateurPage';
import ProfilClientPage from './pages/client/ProfilClientPage';

// ── Pages Directeur d'Agence ──────────────────────────────────────────────────
import DirecteurDashboardPage    from './pages/directeur/DirecteurDashboardPage';
import DirecteurClientsPage      from './pages/directeur/DirecteurClientsPage';
import DirecteurAgentsPage       from './pages/directeur/DirecteurAgentsPage';
import DirecteurValidationsPage  from './pages/directeur/DirecteurValidationsPage';
import DirecteurComptesPage      from './pages/directeur/DirecteurComptesPage';
import DirecteurPretsPage        from './pages/directeur/DirecteurPretsPage';
import DirecteurRapportsPage           from './pages/directeur/DirecteurRapportsPage';
import DirecteurRemboursementsPage    from './pages/directeur/DirecteurRemboursementsPage';
import DirecteurGuichetPage           from './pages/directeur/DirecteurGuichetPage';
import DirecteurClientDetailPage      from './pages/directeur/DirecteurClientDetailPage';

const DASHBOARDS = {
  admin:            '/admin/dashboard',
  agent:            '/agent/dashboard',
  client:           '/client/espace',
  directeur_agence: '/directeur/dashboard',
};

const ProtectedRoute = ({ role, Layout, Page }) => {
  const token = getToken();
  const user  = getCurrentUser();

  if (!token || !user) return <Navigate to="/login" replace />;
  if (user.role !== role) return <Navigate to={DASHBOARDS[user.role] ?? '/login'} replace />;

  return <Layout><Page /></Layout>;
};

const App = () => {
  return (
    <BrowserRouter>
      <Routes>

        {/* ── Redirection racine ─────────────────────────────── */}
        <Route path="/" element={<Navigate to="/login" replace />} />

        {/* ── Auth ──────────────────────────────────────────── */}
        <Route path="/login"            element={<LoginPage />} />
        <Route path="/register"         element={<RegisterPage />} />
        <Route path="/forgot-password"  element={<ForgotPasswordPage />} />
        <Route path="/reset-password"   element={<ResetPasswordPage />} />

        {/* ── Pages ADMIN ───────────────────────────────────── */}
        <Route path="/admin/dashboard"      element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={DashboardPage} />} />
        <Route path="/admin/clients"        element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={ClientsPage} />} />
        <Route path="/admin/clients/:id"    element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={ClientDetailPage} />} />
        <Route path="/admin/prets"          element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={PretsPage} />} />
        <Route path="/admin/prets/:id"      element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={PretDetailPage} />} />
        <Route path="/admin/comptes"        element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={ComptesPage} />} />
        <Route path="/admin/validations"    element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={ValidationsPage} />} />
        <Route path="/admin/transactions"   element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={TransactionsAdminPage} />} />
        <Route path="/admin/remboursements" element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={RemboursementsPage} />} />
        <Route path="/admin/rapports"       element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={RapportsPage} />} />
        <Route path="/admin/notifications"  element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={NotificationsPage} />} />
        <Route path="/admin/agents"         element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={AgentsPage} />} />
        <Route path="/admin/directeurs"     element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={DirecteursPage} />} />
        <Route path="/admin/agences"        element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={AgencesPage} />} />
        <Route path="/admin/parametres"     element={<ProtectedRoute role="admin" Layout={AdminLayout} Page={ParametresPage} />} />

        {/* ── Pages AGENT ───────────────────────────────────── */}
        <Route path="/agent/dashboard"      element={<ProtectedRoute role="agent" Layout={AgentLayout} Page={AgentDashboardPage} />} />
        <Route path="/agent/operations"     element={<ProtectedRoute role="agent" Layout={AgentLayout} Page={OperationsPage} />} />
        <Route path="/agent/clients"        element={<ProtectedRoute role="agent" Layout={AgentLayout} Page={AgentClientsPage} />} />
        <Route path="/agent/clients/:clientId" element={<ProtectedRoute role="agent" Layout={AgentLayout} Page={AgentClientDetailPage} />} />
        <Route path="/agent/prets"          element={<ProtectedRoute role="agent" Layout={AgentLayout} Page={AgentPretsPage} />} />
        <Route path="/agent/remboursements" element={<ProtectedRoute role="agent" Layout={AgentLayout} Page={AgentRemboursementsPage} />} />
        <Route path="/agent/profil"         element={<ProtectedRoute role="agent" Layout={AgentLayout} Page={AgentProfilPage} />} />

        {/* ── Pages CLIENT ──────────────────────────────────── */}
        <Route path="/client/espace"       element={<ProtectedRoute role="client" Layout={ClientLayout} Page={EspacePage} />} />
        <Route path="/client/transactions" element={<ProtectedRoute role="client" Layout={ClientLayout} Page={TransactionsPage} />} />
        <Route path="/client/prets"        element={<ProtectedRoute role="client" Layout={ClientLayout} Page={PretsClientPage} />} />
        <Route path="/client/simulateur"   element={<ProtectedRoute role="client" Layout={ClientLayout} Page={SimulateurPage} />} />
        <Route path="/client/profil"       element={<ProtectedRoute role="client" Layout={ClientLayout} Page={ProfilClientPage} />} />

        {/* ── Pages DIRECTEUR D'AGENCE ──────────────────────── */}
        <Route path="/directeur/dashboard"   element={<ProtectedRoute role="directeur_agence" Layout={DirecteurLayout} Page={DirecteurDashboardPage} />} />
        <Route path="/directeur/clients"     element={<ProtectedRoute role="directeur_agence" Layout={DirecteurLayout} Page={DirecteurClientsPage} />} />
        <Route path="/directeur/clients/:clientId" element={<ProtectedRoute role="directeur_agence" Layout={DirecteurLayout} Page={DirecteurClientDetailPage} />} />
        <Route path="/directeur/agents"      element={<ProtectedRoute role="directeur_agence" Layout={DirecteurLayout} Page={DirecteurAgentsPage} />} />
        <Route path="/directeur/validations" element={<ProtectedRoute role="directeur_agence" Layout={DirecteurLayout} Page={DirecteurValidationsPage} />} />
        <Route path="/directeur/comptes"     element={<ProtectedRoute role="directeur_agence" Layout={DirecteurLayout} Page={DirecteurComptesPage} />} />
        <Route path="/directeur/prets"       element={<ProtectedRoute role="directeur_agence" Layout={DirecteurLayout} Page={DirecteurPretsPage} />} />
        <Route path="/directeur/rapports"         element={<ProtectedRoute role="directeur_agence" Layout={DirecteurLayout} Page={DirecteurRapportsPage} />} />
        <Route path="/directeur/remboursements"   element={<ProtectedRoute role="directeur_agence" Layout={DirecteurLayout} Page={DirecteurRemboursementsPage} />} />
        <Route path="/directeur/guichet"          element={<ProtectedRoute role="directeur_agence" Layout={DirecteurLayout} Page={DirecteurGuichetPage} />} />

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/login" replace />} />

      </Routes>
    </BrowserRouter>
  );
};

export default App;
