import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth, UserRole } from "../hooks/useAuth";

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: UserRole; // 🆕 Use UserRole type
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ 
  children, 
  requiredRole 
}) => {
  const { isLoggedIn, role, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="animate-spin rounded-full h-16 w-16 border-2 border-transparent border-t-yellow-400 border-r-yellow-400"></div>
      </div>
    );
  }

  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }

  // 🆕 FIX: Check role and redirect to correct dashboard
  if (requiredRole && role !== requiredRole) {
    // Redirect based on actual user role
    switch (role) {
      case 'ADMIN':
        return <Navigate to="/admin/dashboard" replace />;
      case 'FREELANCER':
        return <Navigate to="/freelancer/dashboard" replace />;
      case 'EMPLOYER':
        return <Navigate to="/employer/dashboard" replace />;
      case 'USER':
        return <Navigate to="/user/dashboard" replace />;
      default:
        return <Navigate to="/login" replace />;
    }
  }

  return <>{children}</>;
};

export default ProtectedRoute;
