/**
 * ChatPage Component
 * Full page chat interface with conversation list
 */

import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ChatWindow } from '../../components/Chat/ChatWindow';
import { getUserConversations } from '../../services/chatApi';
import { Conversation } from '../../types/chat';

export const ChatPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedConversation, setSelectedConversation] = useState<string | null>(
    searchParams.get('conversationId')
  );

  /**
   * Load user conversations
   */
  useEffect(() => {
    const loadConversations = async () => {
      try {
        setIsLoading(true);
        const data = await getUserConversations();
        setConversations(data);

        // Auto-select first conversation if none selected
        if (!selectedConversation && data.length > 0) {
          setSelectedConversation(data[0].id);
        }
      } catch (err) {
        console.error('Failed to load conversations:', err);
      } finally {
        setIsLoading(false);
      }
    };

    loadConversations();
  }, []);

  /**
   * Update URL when conversation changes
   */
  useEffect(() => {
    if (selectedConversation) {
      setSearchParams({ conversationId: selectedConversation });
    }
  }, [selectedConversation, setSearchParams]);

  return (
    <div className="flex h-[calc(100vh-4rem)] gap-4 p-4">
      {/* Conversation List Sidebar */}
      <div className="w-80 bg-white rounded-lg shadow-lg overflow-hidden flex flex-col">
        <div className="px-4 py-3 border-b bg-gray-50">
          <h2 className="text-lg font-semibold text-gray-800">Conversations</h2>
        </div>

        <div className="flex-1 overflow-y-auto">
          {isLoading ? (
            <div className="flex justify-center items-center h-32">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
          ) : conversations.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <svg className="w-12 h-12 mx-auto mb-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <p className="text-sm">No conversations yet</p>
            </div>
          ) : (
            conversations.map((conv) => (
              <button
                key={conv.id}
                onClick={() => setSelectedConversation(conv.id)}
                className={`
                  w-full text-left px-4 py-3 border-b hover:bg-gray-50 transition-colors
                  ${selectedConversation === conv.id ? 'bg-blue-50 border-l-4 border-blue-600' : ''}
                `}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-gray-800 truncate">
                      Project #{conv.projectId?.substring(0, 8)}
                    </p>
                    <p className="text-sm text-gray-600 truncate mt-1">
                      {conv.lastMessagePreview || 'No messages yet'}
                    </p>
                  </div>
                  {conv.lastMessageAt && (
                    <span className="text-xs text-gray-500 ml-2 flex-shrink-0">
                      {new Date(conv.lastMessageAt).toLocaleDateString()}
                    </span>
                  )}
                </div>
              </button>
            ))
          )}
        </div>
      </div>

      {/* Chat Window */}
      <div className="flex-1">
        {selectedConversation ? (
          <ChatWindow
            conversationId={selectedConversation}
            title={`Conversation - ${selectedConversation.substring(0, 8)}`}
          />
        ) : (
          <div className="h-full bg-white rounded-lg shadow-lg flex items-center justify-center text-gray-500">
            <div className="text-center">
              <svg className="w-20 h-20 mx-auto mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <p className="text-lg">Select a conversation to start chatting</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
