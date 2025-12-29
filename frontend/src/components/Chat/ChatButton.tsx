/**
 * ChatButton Component
 * Opens chat window for a project conversation
 */

import React, { useState, useEffect } from 'react';
import { MessageSquare } from 'lucide-react';
import conversationApi from '../../services/conversationApi';
import { ChatWindow } from './ChatWindow';

interface ChatButtonProps {
  projectId: string;
  projectTitle?: string;
  className?: string;
}

export const ChatButton: React.FC<ChatButtonProps> = ({ 
  projectId, 
  projectTitle,
  className = '' 
}) => {
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Load conversation when button is clicked
   */
  const handleOpenChat = async () => {
    if (conversationId) {
      setIsChatOpen(true);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const conversation = await conversationApi.getConversationByProjectId(projectId);
      setConversationId(conversation.id);
      setIsChatOpen(true);
    } catch (err: any) {
      console.error('Failed to load conversation:', err);
      setError(err.response?.data?.message || 'Failed to load conversation');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      {/* Chat Button */}
      <button
        onClick={handleOpenChat}
        disabled={isLoading}
        className={`
          flex items-center gap-2 px-4 py-2 
          bg-blue-600 text-white rounded-lg 
          hover:bg-blue-700 transition-colors
          disabled:bg-gray-300 disabled:cursor-not-allowed
          ${className}
        `}
      >
        <MessageSquare className="w-5 h-5" />
        {isLoading ? 'Loading...' : 'Open Chat'}
      </button>

      {/* Error Message */}
      {error && (
        <div className="text-sm text-red-600 mt-2">
          {error}
        </div>
      )}

      {/* Chat Modal/Overlay */}
      {isChatOpen && conversationId && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg shadow-2xl w-full max-w-4xl h-[80vh] flex flex-col">
            <ChatWindow
              conversationId={conversationId}
              title={projectTitle ? `Project Chat - ${projectTitle}` : 'Project Chat'}
              onClose={() => setIsChatOpen(false)}
            />
          </div>
        </div>
      )}
    </>
  );
};
