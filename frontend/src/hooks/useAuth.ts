import { useState, useEffect, createContext, useContext } from 'react';
import { userApi, adminApi, authHelper } from '../services/api';

interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: 'USER' | 'ADMIN';
  createdAt: string;
}

interface AuthContextType {
  user: User | null;
  role: 'USER' | 'ADMIN' | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  loading: boolean;
  error: string | null;
  isUser: boolean;
  isAdmin: boolean;
  isLoggedIn: boolean;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}

export function useAuthProvider(): AuthContextType {
  const [user, setUser] = useState<User | null>(null);
  const [role, setRole] = useState<'USER' | 'ADMIN' | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const initAuth = async () => {
      try {
        setLoading(true);
        
        // Check current role from localStorage
        const currentRole = authHelper.getCurrentRole();
        
        if (currentRole) {
          console.log('🔐 Found saved token for role:', currentRole);
          
          try {
            // Always try to get profile to detect role from backend
            const profileResponse = await userApi.getProfile();
            if (profileResponse.success) {
              const p: any = profileResponse.data || {};
              const detectedRole: 'USER' | 'ADMIN' = (p.role === 'ADMIN' || p.isAdmin) ? 'ADMIN' : 'USER';
              setRole(detectedRole);
              setUser({
                id: p.id || 'unknown',
                username: p.username || 'unknown',
                email: p.email || '',
                fullName: p.fullName || p.username || '',
                role: detectedRole,
                createdAt: p.createdAt || new Date().toISOString()
              });

              // Ensure admin token is set for admin client
              if (detectedRole === 'ADMIN') {
                const token = authHelper.getUserToken();
                if (token) adminApi.setToken(token);
              }
            } else {
              console.log('❌ Token invalid, clearing auth');
              authHelper.logoutAll();
              setUser(null);
              setRole(null);
            }
          } catch (err) {
            console.log('❌ Auth check failed:', err);
            authHelper.logoutAll();
            setUser(null);
            setRole(null);
          }
        } else {
          console.log('ℹ️ No saved token found');
        }
      } catch (error) {
        console.error('Auth initialization error:', error);
        setError('Authentication initialization failed');
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  const login = async (username: string, password: string) => {
    try {
      setLoading(true);
      setError(null);
      console.log(`🔑 Attempting login for:`, username);
      const response = await userApi.login(username, password);
      
      if (response.success) {
        console.log('✅ Login successful');
        // Fetch profile to determine role
        const profileResponse = await userApi.getProfile();
        if (profileResponse.success) {
          const p: any = profileResponse.data || {};
          const detectedRole: 'USER' | 'ADMIN' = (p.role === 'ADMIN' || p.isAdmin) ? 'ADMIN' : 'USER';
          setRole(detectedRole);
          setUser({
            id: p.id || 'unknown',
            username: p.username || username,
            email: p.email || '',
            fullName: p.fullName || p.username || username,
            role: detectedRole,
            createdAt: p.createdAt || new Date().toISOString()
          });

          // If admin, mirror token to admin storage
          if (detectedRole === 'ADMIN') {
            const token = authHelper.getUserToken();
            if (token) adminApi.setToken(token);
          }
        } else {
          throw new Error('Failed to fetch profile');
        }
      } else {
        throw new Error(response.message || 'Login failed');
      }
    } catch (error: any) {
      console.error('❌ Login error:', error);
      setError(error.message || 'Login failed');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    console.log('🚪 Logging out...');
    // Fire-and-forget backend logout to blacklist token
    userApi.logout().catch(() => {});
    authHelper.logoutAll();
    setUser(null);
    setRole(null);
    setError(null);
  };

  const isUser = role === 'USER';
  const isAdmin = role === 'ADMIN';
  const isLoggedIn = !!role; // role is set only after successful auth

  return {
    user,
    role,
    login,
    logout,
    loading,
    error,
    isUser,
    isAdmin,
    isLoggedIn
  };
}