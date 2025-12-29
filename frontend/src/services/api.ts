export const API_BASE_URL = 'http://localhost:8080'

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message: string;
  error?: string;
}

export interface FileResponse {
  id: string;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  mimeType: string;
  uploadedBy: string;
  entityType: 'JOB' | 'PROPOSAL' | 'PROJECT' | 'MILESTONE';
  entityId: string;
  createdAt: string;
}

// Base API Client
class BaseApiClient {
  protected baseURL: string;
  protected token: string | null = null;
  protected tokenKey: string = 'token';

  constructor(baseURL: string, tokenKey: string = 'token') {
    this.baseURL = baseURL;
    this.tokenKey = tokenKey;
  }

  setToken(token: string) {
    this.token = token;
    localStorage.setItem(this.tokenKey, token);
  }

  clearToken() {
    this.token = null;
    localStorage.removeItem(this.tokenKey);
  }

  protected loadToken(): string | null {
    // Override this method in subclasses for custom token loading logic
    return localStorage.getItem(this.tokenKey);
  }

  // ✅ Make this public so it can be used from outside
  public async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseURL}${endpoint}`;
    
    // ✅ FIX: Always reload token before each request
    const token = this.loadToken();
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };

    // ✅ FIX: Always add Authorization header if token exists
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers,
        credentials: 'include', // ✅ FIX: Include credentials for cookies
      });

      const contentType = response.headers.get('content-type');
      let data;
      
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
      } else {
        const text = await response.text();
        data = { message: text };
      }

      // ✅ FIX: Handle 401/403 errors with redirect
      if (response.status === 401 || response.status === 403) {
        this.clearToken();
        if (typeof window !== 'undefined' && !endpoint.includes('/login')) {
          // Redirect based on token type
          const redirectPath = this.tokenKey === 'adminToken' ? '/admin/login' : '/login';
          window.location.href = redirectPath;
        }
        return {
          success: false,
          message: 'Session expired. Please login again.',
          error: 'UNAUTHORIZED'
        };
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
    super(API_BASE_URL, 'userToken');
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

  async startTwoFactorSetup() {
    return this.request<any>('/api/auth/2fa/setup', {
      method: 'POST',
    });
  }

  // Confirm enabling TwoFactor with code from authenticator
  async enableTwoFactor(code: string) {
    return this.request<any>('/api/auth/2fa/enable', {
      method: 'POST',
      body: JSON.stringify({ code }),
    });
  }

  // Disable TwoFactor (require password and optionally a code)
  async disableTwoFactor(password: string, code?: string) {
    return this.request<any>('/api/auth/2fa/disable', {
      method: 'POST',
      body: JSON.stringify({ password, code }),
    });
  }

  // 🆕 Freelancer Profile APIs
  async getFreelancerProfile() {
    return this.request<any>('/api/freelancer/profile');
  }

  async updateFreelancerProfile(data: {
    professionalTitle?: string;
    bio?: string;
    hourlyRate?: number;
    availability?: string;
    portfolioUrl?: string;
    linkedinUrl?: string;
    githubUrl?: string;
  }) {
    return this.request<any>('/api/freelancer/profile', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async getFreelancerProfileById(freelancerId: string) {
    return this.request<any>(`/api/freelancer/profile/${freelancerId}`);
  }
}

// ADMIN API CLIENT - Dành riêng cho admin endpoints  
class AdminApiClient extends BaseApiClient {
  constructor() {
    super(API_BASE_URL, 'adminToken');
  }

  // ✅ FIX: Override loadToken to check multiple token sources with proper priority
  protected loadToken(): string | null {
    return (
      localStorage.getItem('adminToken') ||
      localStorage.getItem('userToken') ||
      localStorage.getItem('token')
    );
  }

  // Admin Authentication (if separate from user)
  async adminLogin(username: string, password: string) {
    const response = await this.request<any>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    
    if (response.success && response.data?.token) {
      this.setToken(response.data.token);
      if (response.data.user) {
        localStorage.setItem('adminProfile', JSON.stringify(response.data.user));
      }
    }
    
    return response;
  }

  clearToken() {
    super.clearToken();
    localStorage.removeItem('adminProfile');
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
}

// 🆕 Generic API request helper (uses current user token)
async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const url = `${API_BASE_URL}${endpoint}`;
  
  // ✅ FIX: Check all possible token keys in order
  const token = localStorage.getItem('userToken') || 
                localStorage.getItem('token') || 
                localStorage.getItem('adminToken');
  
  // 🐛 DEBUG: Log token status and decode it
  console.log('🔑 API Request Debug:', {
    endpoint,
    hasToken: !!token,
    tokenKeys: {
      userToken: !!localStorage.getItem('userToken'),
      token: !!localStorage.getItem('token'),
      adminToken: !!localStorage.getItem('adminToken'),
    }
  });
  
  // Decode JWT to check claims
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      console.log('🔓 JWT Payload:', payload);
    } catch (e) {
      console.error('❌ Failed to decode JWT:', e);
    }
  }
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
      credentials: 'include',
    });

    const contentType = response.headers.get('content-type');
    let data;
    
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      const text = await response.text();
      data = { message: text };
    }

    if (response.status === 401 || response.status === 403) {
      if (typeof window !== 'undefined' && !endpoint.includes('/login')) {
        window.location.href = '/login';
      }
      return {
        success: false,
        message: 'Session expired. Please login again.',
        error: 'UNAUTHORIZED'
      };
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

// 🆕 Project & Milestone APIs (UPDATED with all CRUD operations)
export const projectApi = {
  // Get employer's projects
  async getEmployerProjects(page = 0, size = 20) {
    return apiRequest<any>(`/api/projects/employer?page=${page}&size=${size}`);
  },

  // Get freelancer's projects
  async getFreelancerProjects(page = 0, size = 20) {
    return apiRequest<any>(`/api/projects/freelancer?page=${page}&size=${size}`);
  },

  // Get project details
  async getProjectById(projectId: string) {
    return apiRequest<any>(`/api/projects/${projectId}`);
  },

  // Get project milestones
  async getProjectMilestones(projectId: string) {
    return apiRequest<any>(`/api/milestones/project/${projectId}`);
  },

  // 🆕 Create milestone (Employer)
  async createMilestone(projectId: string, data: {
    title: string;
    description?: string;
    amount: number;
    dueDate?: string;
  }) {
    return apiRequest<any>(`/api/milestones/project/${projectId}`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  // 🆕 Update milestone (Employer)
  async updateMilestone(milestoneId: string, data: {
    title?: string;
    description?: string;
    amount?: number;
    dueDate?: string;
  }) {
    return apiRequest<any>(`/api/milestones/${milestoneId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  // 🆕 Delete milestone (Employer)
  async deleteMilestone(milestoneId: string) {
    return apiRequest<any>(`/api/milestones/${milestoneId}`, {
      method: 'DELETE',
    });
  },

  // 🆕 Start working on milestone (Freelancer) - Changes status PENDING → IN_PROGRESS
  async startMilestone(milestoneId: string) {
    return apiRequest<any>(`/api/milestones/${milestoneId}/start`, {
      method: 'POST',
    });
  },

  // 🆕 Submit milestone for review (Freelancer)
  async submitMilestone(milestoneId: string, data: {
    deliverables: string;
    notes?: string;
  }) {
    return apiRequest<any>(`/api/milestones/${milestoneId}/submit`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  // 🆕 Approve milestone (Employer)
  async approveMilestone(milestoneId: string) {
    return apiRequest<any>(`/api/milestones/${milestoneId}/approve`, {
      method: 'POST',
    });
  },

  // 🆕 Reject milestone (Employer)
  async rejectMilestone(milestoneId: string, data: { reason: string }) {
    return apiRequest<any>(`/api/milestones/${milestoneId}/reject`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  // 📎 File Upload
  async uploadFile(file: File, entityType: 'JOB' | 'PROPOSAL' | 'PROJECT' | 'MILESTONE', entityId: string): Promise<FileResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('entityType', entityType);
    formData.append('entityId', entityId);

    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/files/upload`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
      body: formData,
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to upload file');
    }

    return response.json();
  },

  // 📎 Get files by entity
  async getFiles(entityType: 'JOB' | 'PROPOSAL' | 'PROJECT' | 'MILESTONE', entityId: string): Promise<FileResponse[]> {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/api/files/${entityType}/${entityId}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch files');
    }

    return response.json();
  },

  // 📎 Get download URL for a file
  getDownloadUrl(entityType: string, entityId: string, filename: string): string {
    return `${API_BASE_URL}/api/files/download/${entityType}/${entityId}/${filename}`;
  },

  // Release milestone (Employer only) - existing
  async releaseMilestone(milestoneId: string) {
    return apiRequest<any>(`/api/milestones/${milestoneId}/release`, {
      method: 'POST',
    });
  },

  // Get milestone stats
  async getMilestoneStats(projectId: string) {
    return apiRequest<any>(`/api/milestones/project/${projectId}/stats`);
  },
};