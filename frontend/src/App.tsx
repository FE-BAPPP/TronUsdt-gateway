import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { Layout } from "./components/Layout/Layout";
import { Dashboard } from "./pages/Dashboard";
import { Login } from "./pages/Auth/Login";
import { Register } from "./pages/Auth/Register";
import { WalletPage } from "./pages/Wallet/WalletPage";
import { TransactionsPage } from "./pages/Transactions/TransactionsPage";
import { P2PPage } from "./pages/P2P/P2PPage";
import { ProfilePage } from "./pages/Profile/ProfilePage";
import { AdminDashboardPage } from "./pages/Admin/AdminDashboardPage";
import { AdminTrackingPage } from "./pages/Admin/AdminTrackingPage";
import { AdminWithdrawalsPage } from "./pages/Admin/AdminWithdrawalsPage";
import AdminUsersPage from "./pages/Admin/AdminUserPage";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AuthContext, useAuthProvider, useAuth } from "./hooks/useAuth";
import { ForgotPassword } from "./pages/Auth/ForgotPassword";
import { ResetPassword } from "./pages/Auth/ResetPassword";
import { NotificationContainer } from './components/notifications/NotificationContainer';
import { FreelancerDashboard } from './pages/Freelancer/FreelancerDashboard';
import { EmployerDashboard } from './pages/Employer/EmployerDashboard';
import { PostJobPage } from './pages/Employer/PostJobPage';
import { BrowseJobsPage } from './pages/Freelancer/BrowseJobsPage';
import { MyJobsPage } from './pages/Employer/MyJobsPage';
import { JobDetailPage } from './pages/Freelancer/JobDetailPage';
import { MyProposalsPage } from './pages/Freelancer/MyProposalsPage';
import { ViewProposalsPage } from './pages/Employer/ViewProposalsPage';
import { ProjectsPage } from './pages/Projects/ProjectsPage';
import { ProjectDetailPage } from './pages/Projects/ProjectDetailPage';
import { FreelancerProfilePage } from './pages/Freelancer/FreelancerProfilePage';
import { FreelancerPublicProfilePage } from './pages/Freelancer/FreelancerPublicProfilePage';

// Temporary placeholders
const PlaceholderPage = () => <div className="p-8 text-white">This page is under construction</div>;

function AppRoutes() {
  const { isLoggedIn, role } = useAuth();

  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      {/* Root redirect - 🆕 FIX: Check all roles */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            {role === 'ADMIN' ? (
              <Navigate to="/admin/dashboard" replace />
            ) : role === 'FREELANCER' ? (
              <Navigate to="/freelancer/dashboard" replace />
            ) : role === 'EMPLOYER' ? (
              <Navigate to="/employer/dashboard" replace />
            ) : role === 'USER' ? (
              <Navigate to="/user/dashboard" replace />
            ) : (
              <Navigate to="/login" replace />
            )}
          </ProtectedRoute>
        }
      />

      {/* Admin routes */}
      <Route
        path="/admin/*"
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <Layout>
              <Routes>
                <Route path="dashboard" element={<AdminDashboardPage />} />
                <Route path="tracking" element={<AdminTrackingPage />} />
                <Route path="withdrawals" element={<AdminWithdrawalsPage />} />
                <Route path="users" element={<AdminUsersPage />} />
              </Routes>
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* User routes (legacy) */}
      <Route
        path="/user/*"
        element={
          <ProtectedRoute requiredRole="USER">
            <Layout>
              <Routes>
                <Route path="dashboard" element={<Dashboard />} />
                <Route path="wallet" element={<WalletPage />} />
                <Route path="transactions" element={<TransactionsPage />} />
                <Route path="p2p" element={<P2PPage />} />
                <Route path="profile" element={<ProfilePage />} />
              </Routes>
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* 🆕 Freelancer routes */}
      <Route
        path="/freelancer/*"
        element={
          <ProtectedRoute requiredRole="FREELANCER">
            <Layout>
              <Routes>
                <Route path="dashboard" element={<FreelancerDashboard />} />
                <Route path="jobs" element={<BrowseJobsPage />} />
                <Route path="jobs/:id" element={<JobDetailPage />} />
                <Route path="my-proposals" element={<MyProposalsPage />} />
                <Route path="my-projects" element={<ProjectsPage />} />
                <Route path="wallet" element={<WalletPage />} />
                <Route path="profile" element={<ProfilePage />} />
                <Route path="freelancer-profile" element={<FreelancerProfilePage />} />
              </Routes>
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* Employer routes */}
      <Route
        path="/employer/*"
        element={
          <ProtectedRoute requiredRole="EMPLOYER">
            <Layout>
              <Routes>
                <Route path="dashboard" element={<EmployerDashboard />} />
                <Route path="post-job" element={<PostJobPage />} />
                <Route path="my-jobs" element={<MyJobsPage />} />
                <Route path="jobs/:jobId/proposals" element={<ViewProposalsPage />} />
                <Route path="wallet" element={<WalletPage />} />
                <Route path="profile" element={<ProfilePage />} />
              </Routes>
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* 🆕 Project Routes (Both Employer & Freelancer) */}
      <Route
        path="/projects"
        element={
          <ProtectedRoute>
            <Layout>
              <ProjectsPage />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/projects/:projectId"
        element={
          <ProtectedRoute>
            <Layout>
              <ProjectDetailPage />
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* Employer-specific project routes */}
      <Route
        path="/employer/my-projects"
        element={
          <ProtectedRoute allowedRoles={['EMPLOYER']}>
            <Layout>
              <ProjectsPage />
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* Freelancer-specific project routes */}
      <Route
        path="/freelancer/my-projects"
        element={
          <ProtectedRoute allowedRoles={['FREELANCER']}>
            <Layout>
              <ProjectsPage />
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* 🆕 Public Freelancer Profile Route (accessible by Employer/Admin) */}
      <Route
        path="/freelancer/profile/:freelancerId"
        element={
          <ProtectedRoute allowedRoles={['EMPLOYER', 'ADMIN', 'FREELANCER']}>
            <Layout>
              <FreelancerPublicProfilePage />
            </Layout>
          </ProtectedRoute>
        }
      />

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function App() {
  const authValue = useAuthProvider();

  if (authValue.loading) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <AuthContext.Provider value={authValue}>
      <Router>
        <div className="min-h-screen bg-gray-900">
          <AppRoutes />
          
          <NotificationContainer onBalanceUpdate={(balance) => {
            window.dispatchEvent(new CustomEvent('balanceUpdate', { detail: balance }));
          }} />
        </div>
      </Router>
    </AuthContext.Provider>
  );
}

export default App;