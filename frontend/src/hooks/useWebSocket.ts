/**
 * WebSocket Hook
 * Manages STOMP WebSocket connection for real-time messaging
 */

import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Message } from '../types/chat';

interface UseWebSocketProps {
  conversationId?: string;
  onMessageReceived?: (message: Message) => void;
  enabled?: boolean;
}

interface UseWebSocketReturn {
  isConnected: boolean;
  error: string | null;
  reconnect: () => void;
}

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

export const useWebSocket = ({
  conversationId,
  onMessageReceived,
  enabled = true
}: UseWebSocketProps): UseWebSocketReturn => {
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);
  const subscriptionRef = useRef<any>(null);

  const connect = useCallback(() => {
    if (!enabled || !conversationId) {
      return;
    }

    try {
      // Create STOMP client with SockJS
      const client = new Client({
        webSocketFactory: () => new SockJS(WS_URL) as any,
        debug: (str) => {
          console.log('[STOMP Debug]', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,

        onConnect: () => {
          console.log('✅ WebSocket Connected');
          setIsConnected(true);
          setError(null);

          // Subscribe to conversation topic
          if (conversationId) {
            subscriptionRef.current = client.subscribe(
              `/topic/conversation/${conversationId}`,
              (message: IMessage) => {
                try {
                  const receivedMessage: Message = JSON.parse(message.body);
                  console.log('📨 New message received:', receivedMessage);
                  
                  if (onMessageReceived) {
                    onMessageReceived(receivedMessage);
                  }
                } catch (err) {
                  console.error('Failed to parse message:', err);
                }
              }
            );
            console.log(`🔔 Subscribed to conversation: ${conversationId}`);
          }
        },

        onStompError: (frame) => {
          console.error('❌ STOMP Error:', frame);
          setError('Connection error. Retrying...');
          setIsConnected(false);
        },

        onWebSocketClose: () => {
          console.log('🔌 WebSocket Closed');
          setIsConnected(false);
        }
      });

      clientRef.current = client;
      client.activate();
    } catch (err) {
      console.error('Failed to connect WebSocket:', err);
      setError('Failed to establish connection');
    }
  }, [conversationId, enabled, onMessageReceived]);

  const disconnect = useCallback(() => {
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    if (clientRef.current?.active) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }

    setIsConnected(false);
  }, []);

  const reconnect = useCallback(() => {
    disconnect();
    setTimeout(() => connect(), 1000);
  }, [connect, disconnect]);

  // Connect on mount or when conversationId changes
  useEffect(() => {
    connect();

    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    isConnected,
    error,
    reconnect
  };
};
