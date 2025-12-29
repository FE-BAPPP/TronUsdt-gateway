import { API_BASE_URL } from './api';
import { Conversation } from '../types/chat';

class ConversationApiClient {
  private baseURL: string;

  constructor(baseURL: string) {
    this.baseURL = baseURL;
  }

  private getAuthToken(): string | null {
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

    const response = await fetch(url, {
      ...options,
      headers,
      credentials: 'include',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);
    }

    const data = await response.json();
    return data;
  }

  /**
   * 💬 Get conversation by project ID
   */
  async getConversationByProjectId(projectId: string): Promise<Conversation> {
    const response: any = await this.request(`/api/conversations/project/${projectId}`);
    return response.data;
  }

  /**
   * 💬 Get conversation by job ID
   */
  async getConversationByJobId(jobId: string): Promise<Conversation> {
    const response: any = await this.request(`/api/conversations/job/${jobId}`);
    return response.data;
  }

  /**
   * ✅ Check if conversation exists for project
   */
  async checkConversationExists(projectId: string): Promise<boolean> {
    const response: any = await this.request(`/api/conversations/project/${projectId}/exists`);
    return response.data;
  }
}

const conversationApi = new ConversationApiClient(API_BASE_URL);
export default conversationApi;
export { conversationApi };



