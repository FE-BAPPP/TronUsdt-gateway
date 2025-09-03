import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: "USER" | "ADMIN";
  fallback?: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ 
  children, 
  requiredRole, 
  fallback 
}) => {
  const { isLoggedIn, isUser, isAdmin, role } = useAuth();

  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }

  // If no specific role required, allow access
  if (!requiredRole) {
    // Auto-redirect based on role
    if (isAdmin) {
      return <Navigate to="/admin/dashboard" replace />;
    }
    if (isUser) {
      return <Navigate to="/user/dashboard" replace />;
    }
    return <>{children}</>;
  }

  // Check role requirements
  if (requiredRole === "ADMIN" && !isAdmin) {
    return fallback || <Navigate to="/user/dashboard" replace />;
  }

  if (requiredRole === "USER" && !isUser) {
    return fallback || <Navigate to="/admin/dashboard" replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;
