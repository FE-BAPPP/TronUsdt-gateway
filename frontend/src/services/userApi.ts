import { ApiResponse } from './api';

const API_BASE_URL = process.env.NODE_ENV === 'production' 
  ? 'https://your-production-api.com' 
  : 'http://localhost:8080';

class UserApiClient {
  private baseURL: string;
  private token: string | null = null;

  constructor(baseURL: string) {
    this.baseURL = baseURL;
    this.token = localStorage.getItem('userToken');
  }

  setToken(token: string) {
    this.token = token;
    localStorage.setItem('userToken', token);
  }

  clearToken() {
    this.token = null;
    localStorage.removeItem('userToken');
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

  // 1. Authentication APIs
  async login(username: string, password: string) {
    const response = await this.request<any>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    
    if (response.success && (response as any).data?.token) {
      const token = (response as any).data.token;
      this.setToken(token);
    }
    
    return response;
  }

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

  // Forgot/Reset Password
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

  async getPointsHistory(limit: number = 50) {
    return this.request<any>(`/api/points/history?limit=${limit}`);
  }

  async getP2PHistory() {
    return this.request<any>('/api/points/p2p-history');
  }

  async transferPoints(data: {
    toUserId: string;
    amount: number;
    description: string;
  }) {
    return this.request<any>('/api/points/transfer', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getUserStats() {
    return this.request<any>('/api/points/stats');
  }

  // 3. Deposits APIs
  async getDepositHistory(params: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: 'asc' | 'desc';
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

  // 4. Withdrawals APIs
  async createWithdrawalRequest(data: {
    amount: number;
    toAddress: string;
  }) {
    return this.request<any>('/api/withdrawal/request', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getWithdrawalHistory(params: {
    page?: number;
    size?: number;
  } = {}) {
    const { page = 0, size = 20 } = params;
    return this.request<any>(`/api/withdrawal/history?page=${page}&size=${size}`);
  }

  async getWithdrawalStatus(id: string) {
    return this.request<any>(`/api/withdrawal/status/${id}`);
  }

  async getWithdrawalLimits() {
    return this.request<any>('/api/withdrawal/limits');
  }

  // 5. Transactions APIs
  async getAllTransactions(params: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: 'asc' | 'desc';
  } = {}) {
    const { page = 0, size = 10, sortBy = 'createdAt', sortDir = 'desc' } = params;
    return this.request<any>(`/api/transactions?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`);
  }

  async getTransactionDetails(id: string) {
    return this.request<any>(`/api/transactions/${id}`);
  }

  async getTransactionSummary(days: number = 30) {
    return this.request<any>(`/api/transactions/summary?days=${days}`);
  }

  async exportTransactions(format: 'csv' | 'xlsx' = 'csv') {
    return this.request<any>(`/api/transactions/export?format=${format}`);
  }

  // Health Check
  async healthCheck() {
    return this.request<any>('/api/test/health');
  }
}

const userApi = new UserApiClient(API_BASE_URL);
export default userApi;

// Export individual functions for easier usage
export const login = (username: string, password: string) => userApi.login(username, password);
export const register = (data: any) => userApi.register(data);
export const forgotPassword = (email: string) => userApi.forgotPassword(email);
export const resetPassword = (token: string, newPassword: string) => userApi.resetPassword(token, newPassword);
export const getProfile = () => userApi.getProfile();
export const getWallet = () => userApi.getWallet();
export const getDepositAddress = () => userApi.getDepositAddress();
export const getPointsBalance = () => userApi.getPointsBalance();
export const getPointsHistory = (limit?: number) => userApi.getPointsHistory(limit);
export const getP2PHistory = () => userApi.getP2PHistory();
export const transferPoints = (data: any) => userApi.transferPoints(data);
export const getUserStats = () => userApi.getUserStats();
export const getDepositHistory = (params?: any) => userApi.getDepositHistory(params);
export const getPendingDeposits = () => userApi.getPendingDeposits();
export const checkDepositStatus = (txHash: string) => userApi.checkDepositStatus(txHash);
export const createWithdrawalRequest = (data: any) => userApi.createWithdrawalRequest(data);
export const getWithdrawalHistory = (params?: any) => userApi.getWithdrawalHistory(params);
export const getWithdrawalStatus = (id: string) => userApi.getWithdrawalStatus(id);
export const getWithdrawalLimits = () => userApi.getWithdrawalLimits();
export const getAllTransactions = (params?: any) => userApi.getAllTransactions(params);
export const getTransactionDetails = (id: string) => userApi.getTransactionDetails(id);
export const getTransactionSummary = (days?: number) => userApi.getTransactionSummary(days);
export const exportTransactions = (format?: 'csv' | 'xlsx') => userApi.exportTransactions(format);
export const healthCheck = () => userApi.healthCheck();
