// frontend/src/services/jobApi.ts
import { API_BASE_URL } from './api';

export interface Job {
  id: string;
  employerId: string;
  employerName: string;
  title: string;
  description: string;
  type: 'FIXED_PRICE' | 'HOURLY';
  budget: number;
  budgetMin?: number;
  budgetMax?: number;
  currency: string;
  duration?: string;
  deadline?: string;
  status: string;
  skills: string[];
  proposalCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface JobCreateRequest {
  title: string;
  description: string;
  type: 'FIXED_PRICE' | 'HOURLY';
  budget: number;
  budgetMin?: number;
  budgetMax?: number;
  currency?: string;
  duration?: string;
  deadline?: string;
  skillIds?: string[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

class JobApi {
  private getAuthHeaders() {
    const token = localStorage.getItem('token');
    return {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    };
  }

  // POST /api/jobs - Create job (Employer)
  async createJob(request: JobCreateRequest): Promise<{ success: boolean; message: string; data?: Job }> {
    const response = await fetch(`${API_BASE_URL}/api/jobs`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(request)
    });
    return response.json();
  }

  // GET /api/jobs/my-jobs - Employer's jobs
  async getMyJobs(page = 0, size = 10): Promise<PageResponse<Job>> {
    const response = await fetch(
      `${API_BASE_URL}/api/jobs/my-jobs?page=${page}&size=${size}`,
      { headers: this.getAuthHeaders() }
    );
    return response.json();
  }

  // GET /api/jobs - Browse jobs (Freelancer)
  async browseJobs(page = 0, size = 10): Promise<PageResponse<Job>> {
    const response = await fetch(
      `${API_BASE_URL}/api/jobs?page=${page}&size=${size}`,
      { headers: this.getAuthHeaders() }
    );
    return response.json();
  }

  // GET /api/jobs/search?keyword=React
  async searchJobs(keyword: string, page = 0, size = 10): Promise<PageResponse<Job>> {
    const response = await fetch(
      `${API_BASE_URL}/api/jobs/search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`,
      { headers: this.getAuthHeaders() }
    );
    return response.json();
  }

  // GET /api/jobs/{id}
  async getJobById(id: string): Promise<{ success: boolean; data: Job }> {
    const response = await fetch(`${API_BASE_URL}/api/jobs/${id}`, {
      headers: this.getAuthHeaders()
    });
    return response.json();
  }
}

export const jobApi = new JobApi();