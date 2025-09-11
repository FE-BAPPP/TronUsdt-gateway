import { useState, useEffect, useCallback, useRef } from 'react';
import { useAuth } from './useAuth';
import { authHelper } from '../services/api';

export interface NotificationMessage {
  type: 'SYSTEM' | 'DEPOSIT_DETECTED' | 'DEPOSIT_CONFIRMED' | 'WITHDRAWAL_CREATED' | 'WITHDRAWAL_PROCESSING' | 'WITHDRAWAL_COMPLETED' | 'WITHDRAWAL_FAILED' | 'POINTS_TRANSFER' | 'BALANCE_UPDATE';
  title: string;
  message: string;
  txHash?: string;
  withdrawalId?: string;
  amount?: number;
  pointsAmount?: number;
  pointsBalance?: number;
  timestamp: string;
  autoHide?: boolean;
  hideAfterMs?: number;
  id?: string; // Added for auto-hide tracking and duplicate prevention
}

export interface UseNotificationsReturn {
  notifications: NotificationMessage[];
  isConnected: boolean;
  connectionError: string | null;
  clearNotification: (index: number) => void;
  clearAllNotifications: () => void;
  refreshBalance?: () => void;
}

export const useNotifications = (onBalanceUpdate?: (balance: number) => void): UseNotificationsReturn => {
  const { isLoggedIn, user } = useAuth();
  const [notifications, setNotifications] = useState<NotificationMessage[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const maxReconnectAttempts = 5;
  const reconnectAttempts = useRef(0);
  const isConnectingRef = useRef(false); // Prevent multiple simultaneous connections

  // Memoize the balance update callback to prevent reconnections
  const stableOnBalanceUpdate = useRef(onBalanceUpdate);
  stableOnBalanceUpdate.current = onBalanceUpdate;

  const connect = useCallback(() => {
    const token = authHelper.getUserToken();
    if (!isLoggedIn || !token || !user?.id) {
      console.log('🔔 Not authenticated or no user ID, skipping SSE connection');
      return;
    }

    if (eventSourceRef.current || isConnectingRef.current) {
      console.log('🔔 SSE already connected or connecting, readyState:', eventSourceRef.current?.readyState);
      return;
    }

    isConnectingRef.current = true;
    console.log('🔔 Connecting to SSE notifications for user:', user.id);
    
    // EventSource doesn't support custom headers, try different approaches:
    let eventSource: EventSource;
    
    try {
      // Use real endpoint with user ID and username (full URL to backend)
      const userId = user?.id || 'anonymous';
      const username = user?.username || 'unknown';
      const url = `http://localhost:8080/api/notifications/stream?userId=${userId}&username=${username}`;
      eventSource = new EventSource(url, {
        withCredentials: true,
      });
      
      console.log('🔔 Created EventSource for user:', userId, 'readyState:', eventSource.readyState);
    } catch (error) {
      console.error('🔔 Failed to create EventSource:', error);
      setConnectionError('Failed to initialize SSE connection');
      isConnectingRef.current = false;
      return;
    }

    eventSource.onopen = () => {
      console.log('🔔 SSE connected successfully');
      setIsConnected(true);
      setConnectionError(null);
      reconnectAttempts.current = 0;
      isConnectingRef.current = false;
    };

    eventSource.onmessage = (event) => {
      console.log('🔔 SSE raw message:', event);
    };

    eventSource.addEventListener('notification', (event) => {
      try {
        const notification: NotificationMessage = JSON.parse(event.data);
        console.log('🔔 Received notification:', notification);
        
        // Add unique ID for auto-hide tracking
        const notificationWithId = {
          ...notification,
          id: `${notification.type}_${notification.timestamp}_${Math.random().toString(36).substr(2, 9)}`
        };
        
        // Handle notification updates and duplicates
        setNotifications(prev => {
          // Check for duplicates (same type, timestamp, and message)
          const isDuplicate = prev.some(existing => 
            existing.type === notification.type && 
            existing.timestamp === notification.timestamp &&
            existing.message === notification.message
          );
          
          if (isDuplicate) {
            console.log('🔔 Duplicate notification ignored:', notification.type);
            return prev;
          }
          
          // For withdrawal flow: replace PROCESSING with COMPLETED/FAILED for same tx
          if (notification.type === 'WITHDRAWAL_COMPLETED' || notification.type === 'WITHDRAWAL_FAILED') {
            const filteredPrev = prev.filter(existing => 
              !(existing.type === 'WITHDRAWAL_PROCESSING' && existing.txHash === notification.txHash)
            );
            return [notificationWithId, ...filteredPrev.slice(0, 8)]; // Keep space for new one
          }
          
          // For deposit flow: replace DETECTED with CONFIRMED for same tx
          if (notification.type === 'DEPOSIT_CONFIRMED') {
            const filteredPrev = prev.filter(existing => 
              !(existing.type === 'DEPOSIT_DETECTED' && existing.txHash === notification.txHash)
            );
            return [notificationWithId, ...filteredPrev.slice(0, 8)]; // Keep space for new one
          }
          
          return [notificationWithId, ...prev.slice(0, 9)]; // Keep last 10
        });
        
        // Handle balance updates
        if (notification.type === 'BALANCE_UPDATE' && notification.pointsBalance && stableOnBalanceUpdate.current) {
          stableOnBalanceUpdate.current(notification.pointsBalance);
        }
        
        // Auto-hide notifications
        if (notification.autoHide && notification.hideAfterMs) {
          const notificationId = notificationWithId.id;
          console.log(`🔔 Auto-hide scheduled for ${notification.type} in ${notification.hideAfterMs}ms`);
          
          setTimeout(() => {
            console.log(`🔔 Auto-hiding notification: ${notification.type}`);
            setNotifications(prev => {
              const filtered = prev.filter(n => (n as any).id !== notificationId);
              console.log(`🔔 Notifications after auto-hide: ${filtered.length} remaining`);
              return filtered;
            });
          }, notification.hideAfterMs);
        }
        
      } catch (error) {
        console.error('🔔 Error parsing SSE message:', error);
      }
    });

    eventSource.onerror = (error) => {
      console.error('🔔 SSE connection error:', error);
      console.log('🔔 EventSource readyState:', eventSource.readyState);
      console.log('🔔 EventSource url:', eventSource.url);
      
      setIsConnected(false);
      setConnectionError('Connection lost');
      
      // Close and cleanup
      eventSource.close();
      eventSourceRef.current = null;
      isConnectingRef.current = false;
      
      // Attempt reconnection
      if (reconnectAttempts.current < maxReconnectAttempts) {
        reconnectAttempts.current++;
        const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.current), 30000);
        console.log(`🔔 Reconnecting SSE in ${delay}ms (attempt ${reconnectAttempts.current})`);
        
        reconnectTimeoutRef.current = setTimeout(() => {
          connect();
        }, delay);
      } else {
        setConnectionError('Failed to connect after multiple attempts');
      }
    };

    eventSourceRef.current = eventSource;
  }, [isLoggedIn, user?.id, user?.username]);

  const disconnect = useCallback(() => {
    if (eventSourceRef.current) {
      console.log('🔔 Disconnecting SSE, readyState:', eventSourceRef.current.readyState);
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
    
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
    
    setIsConnected(false);
    setConnectionError(null);
    reconnectAttempts.current = 0;
    isConnectingRef.current = false;
  }, []);

  const clearNotification = useCallback((index: number) => {
    setNotifications(prev => prev.filter((_, i) => i !== index));
  }, []);

  const clearAllNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  // Connect on mount and auth change
  useEffect(() => {
    if (isLoggedIn && user?.id) {
      connect();
    } else {
      disconnect();
    }

    return () => {
      disconnect();
    };
  }, [isLoggedIn, user?.id, connect, disconnect]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      disconnect();
    };
  }, [disconnect]);

  return {
    notifications,
    isConnected,
    connectionError,
    clearNotification,
    clearAllNotifications,
  };
};