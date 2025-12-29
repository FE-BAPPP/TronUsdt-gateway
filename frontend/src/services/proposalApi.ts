// frontend/src/services/proposalApi.ts
import { API_BASE_URL } from './api';

export interface ProposalCreateRequest {
  jobId: string;
  coverLetter: string;
  proposedAmount: number;
  estimatedDurationDays: number;
}

export interface ProposalResponse {
  id: string;
  jobId: string;
  jobTitle: string;
  freelancerId: string;
  freelancerName: string;
  freelancerRating?: number;
  freelancerCompletedJobs?: number;
  coverLetter: string;
  proposedAmount: number;
  estimatedDurationDays: number;
  status: string;
  acceptedAt?: string;
  createdAt: string;
  updatedAt: string;
}

class ProposalApiClient {
  private baseURL: string;

  constructor(baseURL: string) {
    this.baseURL = baseURL;
  }

  private getAuthToken(): string | null {
    // ✅ FIX: Read from userToken first (matches UserApiClient)
    return localStorage.getItem('userToken') || 
           localStorage.getItem('token');
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const token = this.getAuthToken();
    const url = `${this.baseURL}${endpoint}`;

    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` }),
      ...(options.headers as Record<string, string>),
    };

    console.log('🌐 Proposal API Request:', {
      url,
      method: options.method || 'GET',
      hasToken: !!token,
      body: options.body
    });

    const response = await fetch(url, {
      ...options,
      headers,
      credentials: 'include',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      console.error('❌ Proposal API Error:', errorData);
      throw new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);
    }

    const data = await response.json();
    console.log('✅ Proposal API Response:', data);
    return data;
  }

  /**
   * Submit a new proposal (Freelancer)
   */
  async submitProposal(data: ProposalCreateRequest): Promise<any> {
    return this.request('/api/proposals', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  /**
   * Get proposals for a job (Employer)
   */
  async getProposalsForJob(jobId: string, page: number = 0, size: number = 20): Promise<any> {
    return this.request(`/api/proposals/job/${jobId}?page=${page}&size=${size}`);
  }

  /**
   * Get freelancer's own proposals
   */
  async getMyProposals(page: number = 0, size: number = 20): Promise<any> {
    return this.request(`/api/proposals/my-proposals?page=${page}&size=${size}`);
  }

  /**
   * Get proposal details
   */
  async getProposalById(proposalId: string): Promise<any> {
    return this.request(`/api/proposals/${proposalId}`);
  }

  /**
   * Award a proposal (Employer)
   */
  async awardProposal(proposalId: string): Promise<any> {
    return this.request(`/api/proposals/${proposalId}/award`, {
      method: 'POST',
    });
  }
}

const proposalApi = new ProposalApiClient(API_BASE_URL);
export default proposalApi;
export { proposalApi };