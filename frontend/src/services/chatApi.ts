/**
 * Chat API Service
 * Handles all message and conversation API calls
 */

import { API_BASE_URL } from './api';
import { 
  Message, 
  MessageSendRequest, 
  MessagePageResponse, 
  Conversation 
} from '../types/chat';

class ChatApiClient {
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
   * 💬 Get messages for a conversation (paginated)
   */
  async getConversationMessages(
    conversationId: string,
    page: number = 0,
    size: number = 50
  ): Promise<MessagePageResponse> {
    const response: any = await this.request(
      `/api/messages/conversation/${conversationId}?page=${page}&size=${size}`
    );
    return response.data;
  }

  /**
   * 📤 Send a message
   */
  async sendMessage(request: MessageSendRequest): Promise<Message> {
    const response: any = await this.request('/api/messages/send', {
      method: 'POST',
      body: JSON.stringify(request),
    });
    return response.data;
  }

  /**
   * ✅ Mark a single message as read
   */
  async markMessageAsRead(messageId: string): Promise<void> {
    await this.request(`/api/messages/${messageId}/read`, {
      method: 'PUT',
    });
  }

  /**
   * ✅ Mark all messages in conversation as read
   */
  async markAllAsRead(conversationId: string): Promise<void> {
    await this.request(`/api/messages/conversation/${conversationId}/read-all`, {
      method: 'PUT',
    });
  }

  /**
   * 📊 Get unread count for a conversation
   */
  async getUnreadCount(conversationId: string): Promise<number> {
    const response: any = await this.request(
      `/api/messages/conversation/${conversationId}/unread-count`
    );
    return response.data;
  }

  /**
   * 📋 Get user's conversations
   */
  async getUserConversations(): Promise<Conversation[]> {
    const response: any = await this.request('/api/messages/conversations');
    return response.data;
  }
}

const chatApi = new ChatApiClient(API_BASE_URL);
export default chatApi;
export { chatApi };

// Export individual functions for backward compatibility
export const getConversationMessages = (conversationId: string, page?: number, size?: number) => 
  chatApi.getConversationMessages(conversationId, page, size);

export const sendMessage = (request: MessageSendRequest) => 
  chatApi.sendMessage(request);

export const markMessageAsRead = (messageId: string) => 
  chatApi.markMessageAsRead(messageId);

export const markAllAsRead = (conversationId: string) => 
  chatApi.markAllAsRead(conversationId);

export const getUnreadCount = (conversationId: string) => 
  chatApi.getUnreadCount(conversationId);

export const getUserConversations = () => 
  chatApi.getUserConversations();
