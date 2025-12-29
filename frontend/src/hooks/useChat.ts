/**
 * Chat Hook
 * Manages chat state, message fetching, and sending
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { Message, MessageSendRequest, MessageType } from '../types/chat';
import { 
  getConversationMessages, 
  sendMessage as sendMessageApi, 
  markAllAsRead,
  getUnreadCount
} from '../services/chatApi';
import { useWebSocket } from './useWebSocket';
import { useAuth } from './useAuth';

interface UseChatProps {
  conversationId: string;
  autoMarkAsRead?: boolean;
}

interface UseChatReturn {
  messages: Message[];
  isLoading: boolean;
  error: string | null;
  hasMore: boolean;
  isConnected: boolean;
  unreadCount: number;
  sendMessage: (content: string, type?: MessageType) => Promise<void>;
  loadMore: () => Promise<void>;
  markAsRead: () => Promise<void>;
  refresh: () => Promise<void>;
}

export const useChat = ({ 
  conversationId, 
  autoMarkAsRead = true 
}: UseChatProps): UseChatReturn => {
  const { user } = useAuth();
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [unreadCount, setUnreadCount] = useState(0);
  const isFetchingRef = useRef(false);

  const hasMore = currentPage < totalPages - 1;

  /**
   * 📨 Handle incoming real-time message
   */
  const handleMessageReceived = useCallback((newMessage: Message) => {
    setMessages((prev) => {
      // Avoid duplicates
      const exists = prev.some(m => m.id === newMessage.id);
      if (exists) return prev;

      // Add to beginning (newest first)
      return [newMessage, ...prev];
    });

    // Update unread count if message is from other user
    if (newMessage.senderId !== user?.id) {
      setUnreadCount(prev => prev + 1);
      
      // Auto mark as read if enabled
      if (autoMarkAsRead) {
        setTimeout(() => markAsRead(), 500);
      }
    }
  }, [user?.id, autoMarkAsRead]);

  /**
   * 🔌 WebSocket connection
   */
  const { isConnected } = useWebSocket({
    conversationId,
    onMessageReceived: handleMessageReceived,
    enabled: !!conversationId
  });

  /**
   * 📥 Fetch messages (paginated)
   */
  const fetchMessages = useCallback(async (page: number = 0) => {
    if (isFetchingRef.current) return;

    try {
      isFetchingRef.current = true;
      setIsLoading(true);
      setError(null);

      const response = await getConversationMessages(conversationId, page, 50);

      setMessages((prev) => {
        if (page === 0) {
          return response.messages;
        }
        // Merge with existing, avoid duplicates
        const newMessages = response.messages.filter(
          newMsg => !prev.some(existingMsg => existingMsg.id === newMsg.id)
        );
        return [...prev, ...newMessages];
      });

      setTotalPages(response.totalPages);
      setCurrentPage(page);
      setUnreadCount(response.unreadCount);
    } catch (err: any) {
      console.error('Failed to fetch messages:', err);
      setError(err.response?.data?.message || 'Failed to load messages');
    } finally {
      setIsLoading(false);
      isFetchingRef.current = false;
    }
  }, [conversationId]);

  /**
   * 📤 Send message
   */
  const sendMessage = useCallback(async (
    content: string, 
    type: MessageType = MessageType.TEXT
  ) => {
    if (!content.trim()) return;

    try {
      setError(null);
      const request: MessageSendRequest = {
        conversationId,
        content: content.trim(),
        messageType: type
      };

      const sentMessage = await sendMessageApi(request);

      // Add to message list immediately (optimistic update)
      setMessages((prev) => [sentMessage, ...prev]);
    } catch (err: any) {
      console.error('Failed to send message:', err);
      setError(err.response?.data?.message || 'Failed to send message');
      throw err;
    }
  }, [conversationId]);

  /**
   * 📖 Load more messages (pagination)
   */
  const loadMore = useCallback(async () => {
    if (!hasMore || isLoading) return;
    await fetchMessages(currentPage + 1);
  }, [hasMore, isLoading, currentPage, fetchMessages]);

  /**
   * ✅ Mark all messages as read
   */
  const markAsRead = useCallback(async () => {
    try {
      await markAllAsRead(conversationId);
      setUnreadCount(0);
      
      // Update local messages
      setMessages(prev => prev.map(msg => ({
        ...msg,
        isRead: msg.senderId !== user?.id ? true : msg.isRead
      })));
    } catch (err) {
      console.error('Failed to mark as read:', err);
    }
  }, [conversationId, user?.id]);

  /**
   * 🔄 Refresh messages
   */
  const refresh = useCallback(async () => {
    setMessages([]);
    setCurrentPage(0);
    await fetchMessages(0);
  }, [fetchMessages]);

  /**
   * Initial load
   */
  useEffect(() => {
    if (conversationId) {
      fetchMessages(0);
    }
  }, [conversationId, fetchMessages]);

  /**
   * Auto mark as read when conversation opens
   */
  useEffect(() => {
    if (conversationId && autoMarkAsRead) {
      const timer = setTimeout(() => markAsRead(), 1000);
      return () => clearTimeout(timer);
    }
  }, [conversationId, autoMarkAsRead, markAsRead]);

  return {
    messages,
    isLoading,
    error,
    hasMore,
    isConnected,
    unreadCount,
    sendMessage,
    loadMore,
    markAsRead,
    refresh
  };
};
