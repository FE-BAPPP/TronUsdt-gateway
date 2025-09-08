import { ApiResponse } from './api';

const API_BASE_URL = process.env.NODE_ENV === 'production'
  ? 'https://your-production-api.com'
  : 'http://localhost:8080';

class AdminApiClient {
  private baseURL: string;
  private token: string | null = null;

  constructor(baseURL: string) {
    this.baseURL = baseURL;
    this.token = localStorage.getItem('adminToken');
  }

  setToken(token: string) {
    this.token = token;
    localStorage.setItem('adminToken', token);
  }

  clearToken() {
    this.token = null;
    localStorage.removeItem('adminToken');
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseURL}${endpoint}`;
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };

    if (this.token) {
      headers.Authorization = `Bearer ${this.token}`;
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }

      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        const data = await response.json();
        return {
          success: true,
          data,
          message: 'Success'
        };
      } else {
        const text = await response.text();
        return {
          success: true,
          data: text as any,
          message: 'Success'
        };
      }
    } catch (error: any) {
      return {
        success: false,
        error: error.message || 'Network error',
        message: error.message || 'Network error'
      };
    }
  }

  // Admin Authentication (same as user but with admin token)
  async adminLogin(username: string, password: string) {
    const response = await this.request<any>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    
    if (response.success && response.data?.token) {
      this.setToken(response.data.token);
    }
    
    return response;
  }

  // Admin Dashboard APIs
  async getDashboardOverview() {
    return this.request<any>('/api/admin/dashboard/overview');
  }

  async getWithdrawalsManagement(params: {
    page?: number;
    size?: number;
  } = {}) {
    const { page = 0, size = 20 } = params;
    return this.request<any>(`/api/admin/dashboard/withdrawals?page=${page}&size=${size}`);
  }

  async getDepositScanStats() {
  // Disabled: some deployments return 403 for this endpoint. UI should use overview data instead.
  return Promise.resolve({ success: false, message: 'Disabled in frontend: deposit scan stats not available' });
  }

  // Admin User Management APIs (if needed)
  async getAllUsers(params: {
    page?: number;
    size?: number;
    search?: string;
  } = {}) {
    const { page = 0, size = 20, search = '' } = params;
    const queryParams = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      ...(search && { search })
    });
    return this.request<any>(`/api/admin/users?${queryParams}`);
  }

  async getUserDetails(userId: string) {
    return this.request<any>(`/api/admin/users/${userId}`);
  }

  async updateUserStatus(userId: string, status: string) {
    return this.request<any>(`/api/admin/users/${userId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    });
  }

  // Admin Transaction Management
  async getAllTransactions(params: {
    page?: number;
    size?: number;
    status?: string;
    type?: string;
    userId?: string;
    startDate?: string;
    endDate?: string;
  } = {}) {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        queryParams.append(key, value.toString());
      }
    });
    return this.request<any>(`/api/admin/transactions?${queryParams}`);
  }

  async approveWithdrawal(withdrawalId: string) {
    return this.request<any>(`/api/admin/withdrawals/${withdrawalId}/approve`, {
      method: 'POST',
    });
  }

  async rejectWithdrawal(withdrawalId: string, reason: string) {
    return this.request<any>(`/api/admin/withdrawals/${withdrawalId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  }

  // Admin Wallet Management
  async getMasterWalletInfo() {
  // Disabled: master wallet endpoint may be restricted. Prefer overview.masterWallet in dashboard.
  return Promise.resolve({ success: false, message: 'Disabled in frontend: master wallet info not available' });
  }

  async getWalletStats() {
  return Promise.resolve({ success: false, message: 'Disabled in frontend: wallet stats not available' });
  }

  async transferFromMasterWallet(data: {
    toAddress: string;
    amount: number;
    reason: string;
  }) {
    return this.request<any>('/api/admin/wallet/transfer', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  // Admin Reports
  async getTransactionReport(params: {
    startDate: string;
    endDate: string;
    type?: string;
    format?: 'csv' | 'xlsx';
  }) {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        queryParams.append(key, value.toString());
      }
    });
    return this.request<any>(`/api/admin/reports/transactions?${queryParams}`);
  }

  async getUserReport(params: {
    startDate?: string;
    endDate?: string;
    status?: string;
    format?: 'csv' | 'xlsx';
  } = {}) {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        queryParams.append(key, value.toString());
      }
    });
    return this.request<any>(`/api/admin/reports/users?${queryParams}`);
  }

  // Admin Settings
  async getSystemSettings() {
    return this.request<any>('/api/admin/settings');
  }

  async updateSystemSettings(settings: Record<string, any>) {
    return this.request<any>('/api/admin/settings', {
      method: 'PUT',
      body: JSON.stringify(settings),
    });
  }

  async getWithdrawalLimits() {
    return this.request<any>('/api/admin/settings/withdrawal-limits');
  }

  async updateWithdrawalLimits(limits: {
    dailyLimit: number;
    monthlyLimit: number;
    minAmount: number;
    maxAmount: number;
  }) {
    return this.request<any>('/api/admin/settings/withdrawal-limits', {
      method: 'PUT',
      body: JSON.stringify(limits),
    });
  }

  // System Health & Monitoring
  async getSystemHealth() {
  // Disabled: health endpoint removed from UI due to permissions
  return Promise.resolve({ success: false, message: 'Disabled in frontend: system health not available' });
  }

  async getSystemLogs(params: {
    level?: 'error' | 'warn' | 'info' | 'debug';
    limit?: number;
    startDate?: string;
    endDate?: string;
  } = {}) {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        queryParams.append(key, value.toString());
      }
    });
    return this.request<any>(`/api/admin/logs?${queryParams}`);
  }

  // Health Check
  async healthCheck() {
    return this.request<any>('/api/test/health');
  }

  // Admin deposits: recent and pending endpoints (added by backend)
  async getRecentDeposits(limit = 50) {
    return this.request<any>(`/api/admin/deposits/recent?limit=${limit}`);
  }

  async getPendingDeposits(limit = 50) {
    return this.request<any>(`/api/admin/deposits/pending?limit=${limit}`);
  }
}

const adminApi = new AdminApiClient(API_BASE_URL);
export default adminApi;

// Export individual functions for easier usage
export const adminLogin = (username: string, password: string) => adminApi.adminLogin(username, password);
export const getDashboardOverview = () => adminApi.getDashboardOverview();
export const getWithdrawalsManagement = (params?: any) => adminApi.getWithdrawalsManagement(params);
export const getAllUsers = (params?: any) => adminApi.getAllUsers(params);
export const getUserDetails = (userId: string) => adminApi.getUserDetails(userId);
export const updateUserStatus = (userId: string, status: string) => adminApi.updateUserStatus(userId, status);
export const getAllTransactions = (params?: any) => adminApi.getAllTransactions(params);
export const approveWithdrawal = (withdrawalId: string) => adminApi.approveWithdrawal(withdrawalId);
export const rejectWithdrawal = (withdrawalId: string, reason: string) => adminApi.rejectWithdrawal(withdrawalId, reason);
export const getMasterWalletInfo = () => adminApi.getMasterWalletInfo();
export const getWalletStats = () => adminApi.getWalletStats();
export const transferFromMasterWallet = (data: any) => adminApi.transferFromMasterWallet(data);
export const getTransactionReport = (params: any) => adminApi.getTransactionReport(params);
export const getUserReport = (params?: any) => adminApi.getUserReport(params);
export const getSystemSettings = () => adminApi.getSystemSettings();
export const updateSystemSettings = (settings: Record<string, any>) => adminApi.updateSystemSettings(settings);
export const getWithdrawalLimits = () => adminApi.getWithdrawalLimits();
export const updateWithdrawalLimits = (limits: any) => adminApi.updateWithdrawalLimits(limits);
export const getSystemHealth = () => adminApi.getSystemHealth();
export const getSystemLogs = (params?: any) => adminApi.getSystemLogs(params);
export const healthCheck = () => adminApi.healthCheck();
export const getRecentDeposits = (limit?: number) => adminApi.getRecentDeposits(limit);
export const getPendingDeposits = (limit?: number) => adminApi.getPendingDeposits(limit);
