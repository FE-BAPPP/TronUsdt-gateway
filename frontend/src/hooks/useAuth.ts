import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { userApi } from '../services/api';

// 🆕 UPDATE: Add FREELANCER and EMPLOYER to role type
export type UserRole = 'USER' | 'ADMIN' | 'FREELANCER' | 'EMPLOYER';

interface AuthContextType {
  user: any;
  role: UserRole | null;
  isLoggedIn: boolean;
  isAdmin: boolean;
  isUser: boolean;
  isFreelancer: boolean; // 🆕 NEW
  isEmployer: boolean;   // 🆕 NEW
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthContext.Provider');
  }
  return context;
};

export const useAuthProvider = (): AuthContextType => {
  const [user, setUser] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  
  // 🆕 FIX: Read role correctly from localStorage
  const storedRole = localStorage.getItem('role') as UserRole | null;
  const role = storedRole || (user?.role as UserRole) || null;

  const isLoggedIn = !!user && !!localStorage.getItem('token');
  const isAdmin = role === 'ADMIN';
  const isUser = role === 'USER';
  const isFreelancer = role === 'FREELANCER'; // 🆕 NEW
  const isEmployer = role === 'EMPLOYER';     // 🆕 NEW

  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('token');
      const storedUser = localStorage.getItem('user');
      const storedRole = localStorage.getItem('role');

      if (token && storedUser && storedRole) {
        try {
          setUser(JSON.parse(storedUser));
        } catch (error) {
          console.error('Failed to parse stored user:', error);
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          localStorage.removeItem('role');
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = async (username: string, password: string) => {
    try {
      const response = await userApi.login(username, password);
      
      if (response.success && response.data) {
        const { token, user: userData } = response.data;
        
        // 🆕 FIX: Store role correctly
        const userRole = userData.role || 'USER';
        
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(userData));
        localStorage.setItem('role', userRole); // ✅ Save FREELANCER/EMPLOYER
        
        setUser(userData);
      } else {
        throw new Error(response.message || 'Login failed');
      }
    } catch (error: any) {
      console.error('Login error:', error);
      throw error;
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('role');
    setUser(null);
    window.location.href = '/login';
  };

  const refreshUser = async () => {
    try {
      const response = await userApi.getProfile();
      if (response.success && response.data) {
        const userData = response.data;
        localStorage.setItem('user', JSON.stringify(userData));
        localStorage.setItem('role', userData.role || 'USER'); // ✅ Update role
        setUser(userData);
      }
    } catch (error) {
      console.error('Failed to refresh user:', error);
    }
  };

  return {
    user,
    role,
    isLoggedIn,
    isAdmin,
    isUser,
    isFreelancer, // 🆕 NEW
    isEmployer,   // 🆕 NEW
    loading,
    login,
    logout,
    refreshUser,
  };
};