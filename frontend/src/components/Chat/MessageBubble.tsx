/**
 * MessageBubble Component
 * Displays a single message with sender info and timestamp
 */

import React from 'react';
import { Message, MessageType } from '../../types/chat';
import { format } from 'date-fns';

interface MessageBubbleProps {
  message: Message;
  isOwn: boolean;
}

export const MessageBubble: React.FC<MessageBubbleProps> = ({ message, isOwn }) => {
  const formattedTime = format(new Date(message.createdAt), 'HH:mm');

  const renderContent = () => {
    switch (message.messageType) {
      case MessageType.IMAGE:
        return (
          <div className="space-y-2">
            {message.attachmentUrl && (
              <img 
                src={message.attachmentUrl} 
                alt="Attachment" 
                className="max-w-sm rounded-lg"
              />
            )}
            {message.content && <p>{message.content}</p>}
          </div>
        );

      case MessageType.FILE:
      case MessageType.DOCUMENT:
        return (
          <div className="space-y-2">
            {message.attachmentUrl && (
              <a 
                href={message.attachmentUrl} 
                target="_blank" 
                rel="noopener noreferrer"
                className="flex items-center gap-2 text-blue-600 hover:underline"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <span>Download File</span>
              </a>
            )}
            {message.content && <p>{message.content}</p>}
          </div>
        );

      default:
        return <p className="whitespace-pre-wrap break-words">{message.content}</p>;
    }
  };

  return (
    <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'} mb-4`}>
      <div className={`max-w-[70%] ${isOwn ? 'order-2' : 'order-1'}`}>
        {/* Sender name (only show for received messages) */}
        {!isOwn && (
          <div className="text-xs text-gray-600 mb-1 px-1">
            {message.senderName}
          </div>
        )}

        {/* Message bubble */}
        <div
          className={`
            rounded-2xl px-4 py-2 shadow-sm
            ${isOwn 
              ? 'bg-blue-600 text-white rounded-br-none' 
              : 'bg-gray-100 text-gray-900 rounded-bl-none'
            }
          `}
        >
          {renderContent()}
        </div>

        {/* Timestamp and read status */}
        <div className={`flex items-center gap-1 mt-1 px-1 ${isOwn ? 'justify-end' : 'justify-start'}`}>
          <span className="text-xs text-gray-500">{formattedTime}</span>
          {isOwn && (
            <span className="text-xs">
              {message.isRead ? (
                <span className="text-blue-500" title="Read">✓✓</span>
              ) : (
                <span className="text-gray-400" title="Sent">✓</span>
              )}
            </span>
          )}
        </div>
      </div>
    </div>
  );
};
