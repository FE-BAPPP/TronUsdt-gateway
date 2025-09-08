export const API_BASE_URL = 'http://localhost:8080';

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message: string;
  error?: string;
}

// Base API Client
class BaseApiClient {
  protected baseURL: string;
  protected token: string | null = null;

  constructor(baseURL: string) {
    this.baseURL = baseURL;
  }

  setToken(token: string) {
    this.token = token;
  }

  clearToken() {
    this.token = null;
  }

  protected async request<T>(
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

      const contentType = response.headers.get('content-type');
      let data;
      
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
      } else {
        const text = await response.text();
        data = { message: text };
      }

      if (!response.ok) {
        return {
          success: false,
          message: data.message || `HTTP ${response.status}`,
          error: data.error
        };
      }

      return {
        success: true,
        data: data.data || data,
        message: data.message || 'Success'
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.message || 'Network error',
        error: error.message
      };
    }
  }
}

// USER API CLIENT - Dành riêng cho user endpoints
class UserApiClient extends BaseApiClient {
  constructor() {
    super(API_BASE_URL);
    // Auto load user token from localStorage
    this.token = localStorage.getItem('userToken');
  }

  setToken(token: string) {
    super.setToken(token);
    localStorage.setItem('userToken', token);
  }

  clearToken() {
    super.clearToken();
    localStorage.removeItem('userToken');
  }

  // 1. Authentication APIs
  async register(data: {
    username: string;
    email: string;
    password: string;
    confirmPassword: string;
    fullName: string;
  }) {
    return this.request<any>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async login(username: string, password: string) {
    const response = await this.request<any>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    
    if (response.success && response.data?.token) {
      this.setToken(response.data.token);
    }
    
    return response;
  }

  async logout() {
    const res = await this.request<any>('/api/auth/logout', { method: 'POST' });
    this.clearToken();
    return res;
  }

  async forgotPassword(email: string) {
    return this.request<any>('/api/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email }),
    });
  }

  async resetPassword(token: string, newPassword: string) {
    return this.request<any>('/api/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ token, newPassword }),
    });
  }

  async getProfile() {
    return this.request<any>('/api/auth/profile');
  }

  async updateProfile(data: { fullName?: string; phone?: string; avatar?: string; description?: string; email?: string; }) {
    return this.request<any>('/api/auth/profile', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async getWallet() {
    return this.request<any>('/api/auth/wallet');
  }

  async getDepositAddress() {
    return this.request<any>('/api/auth/deposit-address');
  }

  // 2. Points Management APIs
  async getPointsBalance() {
    return this.request<any>('/api/points/balance');
  }

  async getPointsHistory(limit = 50) {
    return this.request<any>(`/api/points/history?limit=${limit}`);
  }

  async getP2PHistory() {
    return this.request<any>('/api/points/p2p-history');
  }

  async transferPoints(data: { 
    toUserId: string; 
    amount: number; 
    description?: string;
    password: string;
  }) {
    return this.request<any>('/api/points/transfer', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getUserStats() {
    return this.request<any>('/api/points/stats');
  }

  // Deposits APIs
  async getDepositHistory(params: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  } = {}) {
    const { page = 0, size = 10, sortBy = 'createdAt', sortDir = 'desc' } = params;
    return this.request<any>(`/api/deposits/history?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`);
  }

  async getPendingDeposits() {
    return this.request<any>('/api/deposits/pending');
  }

  async checkDepositStatus(txHash: string) {
    return this.request<any>(`/api/deposits/status/${txHash}`);
  }

  // Withdrawals APIs
  async createWithdrawal(data: {
    amount: number; 
    toAddress: string; 
  }) {
    // create request (PENDING, awaiting confirm)
    return this.request<any>('/api/withdrawal/request', {
      method: 'POST',
      body: JSON.stringify({ amount: data.amount, toAddress: data.toAddress }),
    });
  }

  async confirmWithdrawal(data: {
    withdrawalId: number | string;
    password: string;
    twoFactorCode?: string;
  }) {
    return this.request<any>('/api/withdrawal/confirm', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getWithdrawalHistory(page = 0, size = 20) {
    return this.request<any>(`/api/withdrawal/history?page=${page}&size=${size}`);
  }

  async getWithdrawalStatus(id: string) {
    return this.request<any>(`/api/withdrawal/status/${id}`);
  }

  async getWithdrawalLimits() {
    return this.request<any>('/api/withdrawal/limits');
  }

  async cancelWithdrawal(id: string | number) {
    return this.request<any>(`/api/withdrawal/cancel/${id}`, {
      method: 'POST',
    });
  }

  // Transactions APIs
  async getAllTransactions(params: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  } = {}) {
    const { page = 0, size = 10, sortBy = 'createdAt', sortDir = 'desc' } = params;
    return this.request<any>(`/api/transactions?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`);
  }

  async getTransactionDetails(id: string) {
    return this.request<any>(`/api/transactions/${id}`);
  }

  async getTransactionSummary(days = 30) {
    return this.request<any>(`/api/transactions/summary?days=${days}`);
  }

  async exportTransactions(format = 'csv') {
    return this.request<any>(`/api/transactions/export?format=${format}`);
  }
}

// ADMIN API CLIENT - Dành riêng cho admin endpoints  
class AdminApiClient extends BaseApiClient {
  constructor() {
    super(API_BASE_URL);
    // Auto load admin token from localStorage
    this.token = localStorage.getItem('adminToken');
  }

  setToken(token: string) {
    super.setToken(token);
    localStorage.setItem('adminToken', token);
  }

  clearToken() {
    super.clearToken();
    localStorage.removeItem('adminToken');
  }

  // Admin Authentication (if separate from user)
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

  // Admin Endpoints
  async getDashboardOverview() {
    return this.request<any>('/api/admin/dashboard/overview');
  }

  async getWithdrawalsManagement(page = 0, size = 20) {
    return this.request<any>(`/api/admin/dashboard/withdrawals?page=${page}&size=${size}`);
  }

  async getDepositScanStats() {
  // Disabled: endpoint caused 403 in some environments. Frontend will not call this to avoid noisy errors.
  return Promise.resolve({ success: false, message: 'Disabled in frontend: deposit scan stats not available' });
  }

  // Deposit Scanner Controls
  async scanAddress(address: string, params: { fromBlock?: number; toBlock?: number } = {}) {
    const qp = new URLSearchParams();
    if (typeof params.fromBlock === 'number') qp.append('fromBlock', String(params.fromBlock));
    if (typeof params.toBlock === 'number') qp.append('toBlock', String(params.toBlock));
    const qs = qp.toString();
    return this.request<any>(`/api/admin/deposits/scan/address/${encodeURIComponent(address)}${qs ? `?${qs}` : ''}` , { method: 'POST' });
  }

  async scanBlockRange(fromBlock: number, toBlock: number) {
    const qp = new URLSearchParams({ fromBlock: String(fromBlock), toBlock: String(toBlock) });
    return this.request<any>(`/api/admin/deposits/scan/blocks?${qp.toString()}`, { method: 'POST' });
  }

  async stopDepositScanning() {
    return this.request<any>('/api/admin/deposits/scan/stop', { method: 'POST' });
  }

  // Sweep Stats & Actions
  async getSweepStats() {
  // Disabled: sweep stats endpoint may be restricted; frontend avoids calling it.
  return Promise.resolve({ success: false, message: 'Disabled in frontend: sweep stats not available' });
  }

  async sweepAllDeposits() {
    return this.request<any>('/api/admin/deposits/sweep/all', { method: 'POST' });
  }

  async sweepAddress(address: string) {
    return this.request<any>(`/api/admin/deposits/sweep/address/${encodeURIComponent(address)}`, { method: 'POST' });
  }

  // Additional Admin Endpoints
  async getMasterWalletInfo() {
  // Disabled: use overview.masterWallet from dashboard overview instead.
  return Promise.resolve({ success: false, message: 'Disabled in frontend: master wallet info not available' });
  }

  async checkMasterBalance() {
    return this.request<any>('/api/admin/master/balance');
  }

  async getPoolStats() {
  // Disabled: pool stats endpoint may be restricted; use overview walletPool instead.
  return Promise.resolve({ success: false, message: 'Disabled in frontend: pool stats not available' });
  }

  async getSystemMonitoring() {
    // Disabled: monitoring endpoint removed from UI due to permissions
    return Promise.resolve({ success: false, message: 'Disabled in frontend: system monitoring not available' });
  }

  async getSecurityOverview(_page = 0, _size = 20) {
    // Deprecated: security overview not used in UI
    return Promise.resolve({ success: false, message: 'Disabled in frontend: security overview not available' });
  }

  // Admin deposits: recent and pending (backend provided)
  async getRecentDeposits(limit = 50) {
    return this.request<any>(`/api/admin/deposits/recent?limit=${limit}`);
  }

  async getPendingDeposits(limit = 50) {
    return this.request<any>(`/api/admin/deposits/pending?limit=${limit}`);
  }

  async retryFailedWithdrawals() {
    return this.request<any>(`/api/admin/withdrawals/retry-failed`, { method: 'POST' });
  }

  // New admin withdrawal endpoints (recent list, failed list, retry by id)
  async getRecentWithdrawals(limit = 50) {
    return this.request<any>(`/api/admin/withdrawals/recent?limit=${limit}`);
  }

  async getFailedWithdrawals(limit = 50) {
    return this.request<any>(`/api/admin/withdrawals/failed?limit=${limit}`);
  }

  async retryWithdrawal(id: number | string) {
    return this.request<any>(`/api/admin/withdrawals/retry/${id}`, { method: 'POST' });
  }

  async emergencyStopWithdrawals() {
    return this.request<any>(`/api/admin/withdrawals/emergency-stop`, { method: 'POST' });
  }

  async resumeWithdrawals() {
    return this.request<any>(`/api/admin/withdrawals/resume`, { method: 'POST' });
  }

  // Reset deposit scanner to latestBlock - 50
  async resetDepositScanPosition() {
    return this.request<any>(`/api/admin/dashboard/deposit/scan/reset`, { method: 'POST' });
  }
}

// HEALTH CHECK API - Public endpoint
export const healthApi = {
  async check() {
    try {
      const adminToken = localStorage.getItem('adminToken');
      const userToken = localStorage.getItem('userToken');
      const token = adminToken || userToken;
      const response = await fetch(`${API_BASE_URL}/api/test/health`, {
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      });
      let data: any = {};
      try {
        data = await response.json();
      } catch {
        data = { message: await response.text() };
      }
      return {
        success: response.ok,
        data: data,
        message: response.ok ? 'System healthy' : 'System unhealthy'
      };
    } catch (error: any) {
      return {
        success: false,
        data: null,
        message: error.message || 'Network error'
      };
    }
  }
};

// Export instances
export const userApi = new UserApiClient();
export const adminApi = new AdminApiClient();

// Auth Helper Functions
export const authHelper = {
  // Check if user is logged in
  isUserLoggedIn(): boolean {
    return !!localStorage.getItem('userToken');
  },

  // Check if admin is logged in  
  isAdminLoggedIn(): boolean {
    return !!localStorage.getItem('adminToken');
  },

  // Get current user role
  getCurrentRole(): 'USER' | 'ADMIN' | null {
    if (this.isAdminLoggedIn()) return 'ADMIN';
    if (this.isUserLoggedIn()) return 'USER';
    return null;
  },

  // Logout user
  logoutUser() {
    userApi.clearToken();
    localStorage.removeItem('userProfile');
  },

  // Logout admin
  logoutAdmin() {
    adminApi.clearToken();
    localStorage.removeItem('adminProfile');
  },

  // Logout both
  logoutAll() {
    this.logoutUser();
    this.logoutAdmin();
  },

  // Get user token
  getUserToken(): string | null {
    return localStorage.getItem('userToken');
  },

  // Get admin token
  getAdminToken(): string | null {
    return localStorage.getItem('adminToken');
  }
};