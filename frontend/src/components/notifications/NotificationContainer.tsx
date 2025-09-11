import React from 'react';
import { NotificationToast } from './NotificationToast';
import { useNotifications } from '../../hooks/useNotifications';

interface NotificationContainerProps {
  onBalanceUpdate?: (balance: number) => void;
}

export const NotificationContainer: React.FC<NotificationContainerProps> = ({ onBalanceUpdate }) => {
  const { notifications, isConnected, connectionError, clearNotification } = useNotifications(onBalanceUpdate);

  return (
    <>
      {/* Connection indicator */}
      <div className="fixed top-4 left-4 z-50">
        {connectionError ? (
          <div className="bg-red-900/90 border border-red-500/20 rounded-lg px-3 py-2 text-red-200 text-sm">
            Connection Error: {connectionError}
          </div>
        ) : isConnected ? (
          <div className="bg-green-900/90 border border-green-500/20 rounded-lg px-3 py-2 text-green-200 text-sm">
            🔔 Live notifications connected
          </div>
        ) : (
          <div className="bg-yellow-900/90 border border-yellow-500/20 rounded-lg px-3 py-2 text-yellow-200 text-sm">
            Connecting to live notifications...
          </div>
        )}
      </div>

      {/* Notifications */}
      <div className="fixed top-4 right-4 z-50 space-y-3 max-w-sm w-full">
        {notifications.map((notification, index) => (
          <NotificationToast
            key={`${notification.timestamp}-${index}`}
            notification={notification}
            onClose={() => clearNotification(index)}
          />
        ))}
      </div>
    </>
  );
};