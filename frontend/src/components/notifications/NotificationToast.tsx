import React from 'react';
import { X, CheckCircle, AlertCircle, Clock, CreditCard, ArrowUpRight, ArrowDownLeft, Repeat } from 'lucide-react';
import type { NotificationMessage } from '../../hooks/useNotifications';

interface NotificationToastProps {
  notification: NotificationMessage;
  onClose: () => void;
}

export const NotificationToast: React.FC<NotificationToastProps> = ({ notification, onClose }) => {
  const getIcon = () => {
    switch (notification.type) {
      case 'DEPOSIT_DETECTED':
        return <Clock className="w-5 h-5 text-yellow-400" />;
      case 'DEPOSIT_CONFIRMED':
        return <CheckCircle className="w-5 h-5 text-green-400" />;
      case 'WITHDRAWAL_CREATED':
        return <ArrowUpRight className="w-5 h-5 text-blue-400" />;
      case 'WITHDRAWAL_PROCESSING':
        return <Clock className="w-5 h-5 text-yellow-400" />;
      case 'WITHDRAWAL_COMPLETED':
        return <CheckCircle className="w-5 h-5 text-green-400" />;
      case 'WITHDRAWAL_FAILED':
        return <AlertCircle className="w-5 h-5 text-red-400" />;
      case 'POINTS_TRANSFER':
        return <Repeat className="w-5 h-5 text-purple-400" />;
      case 'BALANCE_UPDATE':
        return <CreditCard className="w-5 h-5 text-blue-400" />;
      default:
        return <AlertCircle className="w-5 h-5 text-gray-400" />;
    }
  };

  const getBgColor = () => {
    switch (notification.type) {
      case 'DEPOSIT_CONFIRMED':
      case 'WITHDRAWAL_COMPLETED':
        return 'bg-green-900/90 border-green-500/20';
      case 'WITHDRAWAL_FAILED':
        return 'bg-red-900/90 border-red-500/20';
      case 'DEPOSIT_DETECTED':
      case 'WITHDRAWAL_PROCESSING':
        return 'bg-yellow-900/90 border-yellow-500/20';
      case 'POINTS_TRANSFER':
        return 'bg-purple-900/90 border-purple-500/20';
      default:
        return 'bg-gray-900/90 border-gray-500/20';
    }
  };

  const formatAmount = (amount?: number) => {
    return amount ? new Intl.NumberFormat('en-US', { 
      minimumFractionDigits: 2, 
      maximumFractionDigits: 6 
    }).format(amount) : '';
  };

  const formatTime = (timestamp: string) => {
    return new Date(timestamp).toLocaleTimeString('en-US', {
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  return (
    <div className={`
      relative p-4 rounded-lg border backdrop-blur-sm shadow-lg
      transform transition-all duration-300 ease-in-out
      hover:scale-105 animate-slide-in
      ${getBgColor()}
    `}>
      <div className="flex items-start gap-3">
        <div className="flex-shrink-0 mt-0.5">
          {getIcon()}
        </div>
        
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between">
            <h4 className="font-semibold text-white text-sm">
              {notification.title}
            </h4>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-white transition-colors ml-2"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
          
          <p className="text-gray-300 text-sm mt-1 leading-relaxed">
            {notification.message}
          </p>
          
          {/* Amount display */}
          {(notification.amount || notification.pointsAmount) && (
            <div className="flex items-center gap-2 mt-2 text-xs">
              {notification.amount && (
                <span className="text-green-400 font-medium">
                  {formatAmount(notification.amount)} USDT
                </span>
              )}
              {notification.pointsAmount && (
                <span className="text-blue-400 font-medium">
                  {formatAmount(notification.pointsAmount)} Points
                </span>
              )}
            </div>
          )}
          
          {/* Transaction hash (clickable) */}
          {notification.txHash && (
            <div className="mt-2">
              <a 
                href={`https://nile.tronscan.org/#/transaction/${notification.txHash}`}
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-400 hover:text-blue-300 text-xs font-mono break-all transition-colors"
              >
                {notification.txHash.substring(0, 20)}...
              </a>
            </div>
          )}
          
          <div className="flex items-center justify-between mt-2">
            <span className="text-gray-500 text-xs">
              {formatTime(notification.timestamp)}
            </span>
            
            {!notification.autoHide && (
              <span className="text-gray-500 text-xs">
                Click to dismiss
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};