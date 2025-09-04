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
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AuthContext, useAuthProvider } from "./hooks/useAuth";
import { ForgotPassword } from "./pages/Auth/ForgotPassword";
import { ResetPassword } from "./pages/Auth/ResetPassword";

function AppRoutes() {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      {/* Protected User Routes */}
      <Route path="/user/dashboard" element={
        <ProtectedRoute requiredRole="USER">
          <Layout>
            <Dashboard />
          </Layout>
        </ProtectedRoute>
      } />
      
      <Route path="/user/wallet" element={
        <ProtectedRoute requiredRole="USER">
          <Layout>
            <WalletPage />
          </Layout>
        </ProtectedRoute>
      } />
      
      <Route path="/user/transactions" element={
        <ProtectedRoute requiredRole="USER">
          <Layout>
            <TransactionsPage />
          </Layout>
        </ProtectedRoute>
      } />
      
      <Route path="/user/p2p" element={
        <ProtectedRoute requiredRole="USER">
          <Layout>
            <P2PPage />
          </Layout>
        </ProtectedRoute>
      } />
      
      <Route path="/user/profile" element={
        <ProtectedRoute requiredRole="USER">
          <Layout>
            <ProfilePage />
          </Layout>
        </ProtectedRoute>
      } />

      {/* Protected Admin Routes */}
      <Route path="/admin/dashboard" element={
        <ProtectedRoute requiredRole="ADMIN">
          <Layout>
            <AdminDashboardPage />
          </Layout>
        </ProtectedRoute>
      } />

      <Route path="/admin/tracking" element={
        <ProtectedRoute requiredRole="ADMIN">
          <Layout>
            <AdminTrackingPage />
          </Layout>
        </ProtectedRoute>
      } />

      <Route path="/admin/withdrawals" element={
        <ProtectedRoute requiredRole="ADMIN">
          <Layout>
            <AdminWithdrawalsPage />
          </Layout>
        </ProtectedRoute>
      } />

      {/* Redirects based on authentication */}
      <Route path="/" element={<RoleBasedRedirect />} />
      <Route path="/dashboard" element={<RoleBasedRedirect />} />
      
      {/* Fallback */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function RoleBasedRedirect() {
  return (
    <ProtectedRoute>
      <RedirectToRole />
    </ProtectedRoute>
  );
}

function RedirectToRole() {
  return null; 
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
        </div>
      </Router>
    </AuthContext.Provider>
  );
}

export default App;