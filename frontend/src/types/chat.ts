/**
 * Chat & Messaging Type Definitions
 */

export enum MessageType {
  TEXT = 'TEXT',
  FILE = 'FILE',
  IMAGE = 'IMAGE',
  DOCUMENT = 'DOCUMENT'
}

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  content: string;
  messageType: MessageType;
  attachmentUrl?: string;
  isRead: boolean;
  createdAt: string;
  readAt?: string;
}

export interface Conversation {
  id: string;
  jobId?: string;
  projectId?: string;
  createdAt: string;
  lastMessageAt?: string;
  lastMessagePreview?: string;
  unreadCount?: number;
}

export interface MessageSendRequest {
  conversationId: string;
  content: string;
  messageType?: MessageType;
  attachmentUrl?: string;
}

export interface MessagePageResponse {
  messages: Message[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
  unreadCount: number;
}

export interface ConversationWithDetails extends Conversation {
  otherPartyName?: string;
  otherPartyId?: string;
  projectTitle?: string;
}
