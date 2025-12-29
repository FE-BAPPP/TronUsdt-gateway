package com.UsdtWallet.UsdtWallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.UsdtWallet.UsdtWallet.model.entity.Notification;
import com.UsdtWallet.UsdtWallet.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final NotificationRepository notificationRepository;
    
    // Store active SSE connections by userId
    private final Map<UUID, List<SseEmitter>> userConnections = new ConcurrentHashMap<>();

    // ============= SSE CONNECTION MANAGEMENT =============
    
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

    // ============= PERSISTENT NOTIFICATION MANAGEMENT =============
    
    /**
     * Create and persist notification in database
     */
    @Transactional
    public Notification createNotification(
            UUID userId,
            Notification.NotificationType type,
            String title,
            String message,
            String entityType,
            UUID entityId) {
        
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .entityType(entityType)
                .entityId(entityId)
                .isRead(false)
                .build();
        
        notification = notificationRepository.save(notification);
        
        // Send real-time notification via SSE
        sendToUser(userId, convertToMessage(notification));
        
        log.info("Created notification: {} for user: {}", type, userId);
        return notification;
    }
    
    /**
     * Get user notifications with pagination
     */
    public Page<Notification> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * Get unread notifications count
     */
    public Long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.findByIdAndUserId(notificationId, userId)
                .ifPresent(notification -> {
                    notification.setIsRead(true);
                    notification.setReadAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                });
    }
    
    /**
     * Mark all notifications as read
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unread.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        });
        notificationRepository.saveAll(unread);
    }

    // ============= WALLET & PAYMENT NOTIFICATIONS =============

    public void notifyDepositDetected(UUID userId, String txHash, BigDecimal amount) {
        sendToUser(userId, NotificationMessage.builder()
            .type(NotificationType.DEPOSIT_DETECTED)
            .title("Deposit Detected")
            .message(String.format("%.2f USDT deposit detected", amount))
            .txHash(txHash)
            .amount(amount)
            .timestamp(LocalDateTime.now())
            .autoHide(true)
            .hideAfterMs(12000)
            .build());
    }

    public void notifyDepositConfirmed(UUID userId, String txHash, BigDecimal amount, BigDecimal pointsCredited) {
        // Send SSE notification
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
        
        // Persist notification
        createNotification(
            userId,
            Notification.NotificationType.DEPOSIT_SUCCESS,
            "Deposit Confirmed",
            String.format("%.2f USDT confirmed and credited to your account", pointsCredited),
            "PAYMENT",
            null
        );
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
            .hideAfterMs(15000)
            .build());
    }

    public void notifyWithdrawalCompleted(UUID userId, String txHash, BigDecimal amount) {
        // Send SSE notification
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
        
        // Persist notification
        createNotification(
            userId,
            Notification.NotificationType.WITHDRAWAL_SUCCESS,
            "Withdrawal Completed",
            String.format("%.2f USDT withdrawal completed successfully", amount),
            "PAYMENT",
            null
        );
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

    // ============= JOB & PROPOSAL NOTIFICATIONS =============
    
    public void notifyJobPosted(UUID employerId, UUID jobId, String jobTitle) {
        createNotification(
            employerId,
            Notification.NotificationType.JOB_POSTED,
            "Job Posted Successfully",
            String.format("Your job '%s' has been posted and is now visible to freelancers", jobTitle),
            "JOB",
            jobId
        );
    }
    
    public void notifyProposalReceived(UUID employerId, UUID jobId, String freelancerName, String jobTitle) {
        createNotification(
            employerId,
            Notification.NotificationType.PROPOSAL_RECEIVED,
            "New Proposal Received",
            String.format("%s has submitted a proposal for '%s'", freelancerName, jobTitle),
            "JOB",
            jobId
        );
    }
    
    public void notifyProposalAccepted(UUID freelancerId, UUID projectId, String jobTitle) {
        createNotification(
            freelancerId,
            Notification.NotificationType.PROPOSAL_ACCEPTED,
            "Proposal Accepted! 🎉",
            String.format("Congratulations! Your proposal for '%s' has been accepted", jobTitle),
            "PROJECT",
            projectId
        );
    }
    
    public void notifyProposalRejected(UUID freelancerId, UUID jobId, String jobTitle) {
        createNotification(
            freelancerId,
            Notification.NotificationType.PROPOSAL_ACCEPTED, // Reuse for rejected
            "Proposal Status Update",
            String.format("Your proposal for '%s' was not selected this time", jobTitle),
            "JOB",
            jobId
        );
    }

    // ============= PROJECT & MILESTONE NOTIFICATIONS =============
    
    public void notifyProjectStarted(UUID freelancerId, UUID employerId, UUID projectId, String projectTitle) {
        // Notify freelancer
        createNotification(
            freelancerId,
            Notification.NotificationType.PROJECT_STARTED,
            "Project Started",
            String.format("Project '%s' has started. Good luck!", projectTitle),
            "PROJECT",
            projectId
        );
        
        // Notify employer
        createNotification(
            employerId,
            Notification.NotificationType.PROJECT_STARTED,
            "Project Started",
            String.format("Project '%s' has started with your selected freelancer", projectTitle),
            "PROJECT",
            projectId
        );
    }
    
    public void notifyMilestoneCreated(UUID freelancerId, UUID milestoneId, String milestoneTitle, BigDecimal amount) {
        createNotification(
            freelancerId,
            Notification.NotificationType.MILESTONE_CREATED,
            "New Milestone Created",
            String.format("Milestone '%s' (%.2f USDT) has been created", milestoneTitle, amount),
            "MILESTONE",
            milestoneId
        );
    }
    
    public void notifyMilestoneFunded(UUID freelancerId, UUID milestoneId, String milestoneTitle, BigDecimal amount) {
        createNotification(
            freelancerId,
            Notification.NotificationType.MILESTONE_CREATED,
            "Milestone Funded",
            String.format("Milestone '%s' has been funded with %.2f USDT", milestoneTitle, amount),
            "MILESTONE",
            milestoneId
        );
    }
    
    public void notifyMilestoneSubmitted(UUID employerId, UUID milestoneId, String milestoneTitle) {
        createNotification(
            employerId,
            Notification.NotificationType.MILESTONE_SUBMITTED,
            "Milestone Submitted for Review",
            String.format("Milestone '%s' has been submitted and awaits your approval", milestoneTitle),
            "MILESTONE",
            milestoneId
        );
    }
    
    public void notifyMilestoneApproved(UUID freelancerId, UUID milestoneId, String milestoneTitle, BigDecimal amount) {
        createNotification(
            freelancerId,
            Notification.NotificationType.MILESTONE_APPROVED,
            "Milestone Approved! 🎉",
            String.format("Milestone '%s' approved! %.2f USDT has been released to your account", 
                milestoneTitle, amount),
            "MILESTONE",
            milestoneId
        );
    }
    
    public void notifyMilestoneRejected(UUID freelancerId, UUID milestoneId, String milestoneTitle, String reason) {
        createNotification(
            freelancerId,
            Notification.NotificationType.MILESTONE_REJECTED,
            "Milestone Requires Changes",
            String.format("Milestone '%s' needs revision. Reason: %s", milestoneTitle, reason),
            "MILESTONE",
            milestoneId
        );
    }

    // ============= PAYMENT NOTIFICATIONS =============
    
    public void notifyPaymentReceived(UUID userId, BigDecimal amount, String from, String description) {
        createNotification(
            userId,
            Notification.NotificationType.PAYMENT_RECEIVED,
            "Payment Received",
            String.format("You received %.2f USDT from %s. %s", amount, from, description),
            "PAYMENT",
            null
        );
    }
    
    public void notifyPaymentSent(UUID userId, BigDecimal amount, String to, String description) {
        createNotification(
            userId,
            Notification.NotificationType.PAYMENT_SENT,
            "Payment Sent",
            String.format("You sent %.2f USDT to %s. %s", amount, to, description),
            "PAYMENT",
            null
        );
    }

    // ============= DISPUTE NOTIFICATIONS =============
    
    public void notifyDisputeOpened(UUID userId, UUID disputeId, String projectTitle) {
        createNotification(
            userId,
            Notification.NotificationType.DISPUTE_OPENED,
            "Dispute Opened",
            String.format("A dispute has been opened for project '%s'. An admin will review it shortly.", 
                projectTitle),
            "DISPUTE",
            disputeId
        );
    }
    
    public void notifyDisputeResolved(UUID userId, UUID disputeId, String resolution) {
        createNotification(
            userId,
            Notification.NotificationType.DISPUTE_RESOLVED,
            "Dispute Resolved",
            String.format("The dispute has been resolved. Resolution: %s", resolution),
            "DISPUTE",
            disputeId
        );
    }

    // ============= MESSAGE NOTIFICATIONS =============
    
    /**
     * 💬 Notify user about new message
     */
    public void notifyNewMessage(UUID userId, UUID conversationId, String senderName, String preview) {
        createNotification(
            userId,
            Notification.NotificationType.MESSAGE_RECEIVED,
            "New Message from " + senderName,
            preview,
            "CONVERSATION",
            conversationId
        );
        
        // Send real-time SSE notification
        sendToUser(userId, NotificationMessage.builder()
            .type(NotificationType.MESSAGE)
            .title("New Message")
            .message(senderName + ": " + preview)
            .entityId(conversationId.toString())
            .timestamp(LocalDateTime.now())
            .build());
    }

    // ============= HELPER METHODS =============
    
    /**
     * Convert persistent Notification entity to SSE NotificationMessage
     */
    private NotificationMessage convertToMessage(Notification notification) {
        return NotificationMessage.builder()
            .type(mapToSseType(notification.getType()))
            .title(notification.getTitle())
            .message(notification.getMessage())
            .timestamp(notification.getCreatedAt())
            .autoHide(true)
            .hideAfterMs(8000)
            .build();
    }
    
    /**
     * Map persistent notification type to SSE notification type
     */
    private NotificationType mapToSseType(Notification.NotificationType dbType) {
        return switch (dbType) {
            case DEPOSIT_SUCCESS -> NotificationType.DEPOSIT_CONFIRMED;
            case WITHDRAWAL_SUCCESS -> NotificationType.WITHDRAWAL_COMPLETED;
            case WITHDRAWAL_PENDING -> NotificationType.WITHDRAWAL_PROCESSING;
            default -> NotificationType.SYSTEM;
        };
    }

    // ============= NOTIFICATION MESSAGE CLASSES (SSE) =============

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

        public NotificationMessageBuilder entityId(String string) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'entityId'");
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
        BALANCE_UPDATE,
        MESSAGE // Added MESSAGE type for chat notifications
    }
}