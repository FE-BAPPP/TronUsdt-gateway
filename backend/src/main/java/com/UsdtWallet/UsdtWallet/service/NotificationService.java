package com.UsdtWallet.UsdtWallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final ObjectMapper objectMapper;
    
    // Store active SSE connections by userId
    private final Map<UUID, List<SseEmitter>> userConnections = new ConcurrentHashMap<>();

    public void addUserConnection(UUID userId, SseEmitter emitter) {
        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.debug("Added SSE connection for user: {}, total connections: {}", 
                 userId, userConnections.get(userId).size());
        
        // Send initial connection success message
        sendToUser(userId, NotificationMessage.builder()
            .type(NotificationType.SYSTEM)
            .title("Connection Established")
            .message("Real-time notifications connected")
            .timestamp(LocalDateTime.now())
            .build());
    }

    public void removeUserConnection(UUID userId, SseEmitter emitter) {
        List<SseEmitter> connections = userConnections.get(userId);
        if (connections != null) {
            connections.remove(emitter);
            if (connections.isEmpty()) {
                userConnections.remove(userId);
            }
            log.debug("Removed SSE connection for user: {}", userId);
        }
    }

    public void sendToUser(UUID userId, NotificationMessage message) {
        List<SseEmitter> connections = userConnections.get(userId);
        if (connections == null || connections.isEmpty()) {
            log.debug("No SSE connections for user: {}", userId);
            return;
        }

        List<SseEmitter> deadConnections = new ArrayList<>();
        
        for (SseEmitter emitter : connections) {
            try {
                String eventData = objectMapper.writeValueAsString(message);
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(eventData));
                    
                log.debug("Sent notification to user {}: {}", userId, message.getTitle());
            } catch (Exception e) {
                log.warn("Failed to send notification to user: {}", userId, e);
                deadConnections.add(emitter);
            }
        }
        
        // Clean up dead connections
        deadConnections.forEach(emitter -> removeUserConnection(userId, emitter));
    }

    // Notification methods for different events
    public void notifyDepositDetected(UUID userId, String txHash, BigDecimal amount) {
        sendToUser(userId, NotificationMessage.builder()
            .type(NotificationType.DEPOSIT_DETECTED)
            .title("Deposit Detected")
            .message(String.format("%.2f USDT deposit detected", amount))
            .txHash(txHash)
            .amount(amount)
            .timestamp(LocalDateTime.now())
            .autoHide(true)
            .hideAfterMs(12000) // Auto-hide after 12 seconds
            .build());
    }

    public void notifyDepositConfirmed(UUID userId, String txHash, BigDecimal amount, BigDecimal pointsCredited) {
        sendToUser(userId, NotificationMessage.builder()
            .type(NotificationType.DEPOSIT_CONFIRMED)
            .title("Deposit Confirmed")
            .message(String.format("%.2f USDT confirmed, %.2f points credited", amount, pointsCredited))
            .txHash(txHash)
            .amount(amount)
            .pointsAmount(pointsCredited)
            .timestamp(LocalDateTime.now())
            .autoHide(true)
            .hideAfterMs(10000)
            .build());
    }

    public void notifyWithdrawalCreated(UUID userId, String withdrawalId, BigDecimal amount) {
        sendToUser(userId, NotificationMessage.builder()
            .type(NotificationType.WITHDRAWAL_CREATED)
            .title("Withdrawal Request Created")
            .message(String.format("Withdrawal request for %.2f USDT created", amount))
            .withdrawalId(withdrawalId)
            .amount(amount)
            .timestamp(LocalDateTime.now())
            .autoHide(true)
            .hideAfterMs(5000)
            .build());
    }

    public void notifyWithdrawalProcessing(UUID userId, String withdrawalId, String txHash, BigDecimal amount) {
        sendToUser(userId, NotificationMessage.builder()
            .type(NotificationType.WITHDRAWAL_PROCESSING)
            .title("Withdrawal Processing")
            .message(String.format("Your %.2f USDT withdrawal is being processed", amount))
            .txHash(txHash)
            .withdrawalId(withdrawalId)
            .amount(amount)
            .timestamp(LocalDateTime.now())
            .autoHide(true)
            .hideAfterMs(15000) // Auto-hide after 15 seconds
            .build());
    }

    public void notifyWithdrawalCompleted(UUID userId, String txHash, BigDecimal amount) {
        sendToUser(userId, NotificationMessage.builder()
            .type(NotificationType.WITHDRAWAL_COMPLETED)
            .title("Withdrawal Completed")
            .message(String.format("%.2f USDT withdrawal completed successfully", amount))
            .txHash(txHash)
            .amount(amount)
            .timestamp(LocalDateTime.now())
            .autoHide(true)
            .hideAfterMs(10000)
            .build());
    }

    public void notifyPointsTransferred(UUID userId, BigDecimal amount, String fromTo, boolean isReceived) {
        String action = isReceived ? "received from" : "sent to";
        sendToUser(userId, NotificationMessage.builder()
            .type(NotificationType.POINTS_TRANSFER)
            .title("Points Transfer")
            .message(String.format("%.2f points %s %s", amount, action, fromTo))
            .pointsAmount(amount)
            .timestamp(LocalDateTime.now())
            .autoHide(true)
            .hideAfterMs(7000)
            .build());
    }

    public void notifyBalanceUpdate(UUID userId, BigDecimal newBalance) {
        sendToUser(userId, NotificationMessage.builder()
            .type(NotificationType.BALANCE_UPDATE)
            .title("Balance Updated")
            .message("Your balance has been updated")
            .pointsBalance(newBalance)
            .timestamp(LocalDateTime.now())
            .autoHide(true)
            .hideAfterMs(3000)
            .build());
    }

    // Placeholder methods for unread count and marking as read
    public Long getUnreadCount(UUID userId) {
        
        return 0L;
    }

    public void markAsRead(Long notificationId, UUID userId) {
    
    }

    public void markAllAsRead(UUID userId) {
        
    }

    // Notification message structure
    public static class NotificationMessage {
        private NotificationType type;
        private String title;
        private String message;
        private String txHash;
        private String withdrawalId;
        private BigDecimal amount;
        private BigDecimal pointsAmount;
        private BigDecimal pointsBalance;
        private LocalDateTime timestamp;
        private boolean autoHide = true;
        private long hideAfterMs = 5000;

        public static NotificationMessageBuilder builder() {
            return new NotificationMessageBuilder();
        }

        // Getters and setters
        public NotificationType getType() { return type; }
        public void setType(NotificationType type) { this.type = type; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getTxHash() { return txHash; }
        public void setTxHash(String txHash) { this.txHash = txHash; }
        public String getWithdrawalId() { return withdrawalId; }
        public void setWithdrawalId(String withdrawalId) { this.withdrawalId = withdrawalId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public BigDecimal getPointsAmount() { return pointsAmount; }
        public void setPointsAmount(BigDecimal pointsAmount) { this.pointsAmount = pointsAmount; }
        public BigDecimal getPointsBalance() { return pointsBalance; }
        public void setPointsBalance(BigDecimal pointsBalance) { this.pointsBalance = pointsBalance; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public boolean isAutoHide() { return autoHide; }
        public void setAutoHide(boolean autoHide) { this.autoHide = autoHide; }
        public long getHideAfterMs() { return hideAfterMs; }
        public void setHideAfterMs(long hideAfterMs) { this.hideAfterMs = hideAfterMs; }
    }

    public static class NotificationMessageBuilder {
        private final NotificationMessage message = new NotificationMessage();

        public NotificationMessageBuilder type(NotificationType type) {
            message.setType(type);
            return this;
        }

        public NotificationMessageBuilder title(String title) {
            message.setTitle(title);
            return this;
        }

        public NotificationMessageBuilder message(String msg) {
            message.setMessage(msg);
            return this;
        }

        public NotificationMessageBuilder txHash(String txHash) {
            message.setTxHash(txHash);
            return this;
        }

        public NotificationMessageBuilder withdrawalId(String withdrawalId) {
            message.setWithdrawalId(withdrawalId);
            return this;
        }

        public NotificationMessageBuilder amount(BigDecimal amount) {
            message.setAmount(amount);
            return this;
        }

        public NotificationMessageBuilder pointsAmount(BigDecimal pointsAmount) {
            message.setPointsAmount(pointsAmount);
            return this;
        }

        public NotificationMessageBuilder pointsBalance(BigDecimal pointsBalance) {
            message.setPointsBalance(pointsBalance);
            return this;
        }

        public NotificationMessageBuilder timestamp(LocalDateTime timestamp) {
            message.setTimestamp(timestamp);
            return this;
        }

        public NotificationMessageBuilder autoHide(boolean autoHide) {
            message.setAutoHide(autoHide);
            return this;
        }

        public NotificationMessageBuilder hideAfterMs(long hideAfterMs) {
            message.setHideAfterMs(hideAfterMs);
            return this;
        }

        public NotificationMessage build() {
            return message;
        }
    }

    public enum NotificationType {
        SYSTEM,
        DEPOSIT_DETECTED,
        DEPOSIT_CONFIRMED,
        WITHDRAWAL_CREATED,
        WITHDRAWAL_PROCESSING,
        WITHDRAWAL_COMPLETED,
        WITHDRAWAL_FAILED,
        POINTS_TRANSFER,
        BALANCE_UPDATE
    }
}