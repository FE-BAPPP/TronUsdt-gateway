package com.UsdtWallet.UsdtWallet.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "entity_type")
    private String entityType; // JOB, PROJECT, MILESTONE, PAYMENT
    
    @Column(name = "entity_id")
    private UUID entityId;
    
    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum NotificationType {
        DEPOSIT_SUCCESS,
        WITHDRAWAL_SUCCESS,
        WITHDRAWAL_PENDING,
        JOB_POSTED,
        PROPOSAL_RECEIVED,
        PROPOSAL_ACCEPTED,
        PROJECT_STARTED,
        MILESTONE_CREATED,
        MILESTONE_SUBMITTED,
        MILESTONE_APPROVED,
        MILESTONE_REJECTED,
        PAYMENT_RECEIVED,
        PAYMENT_SENT,
        DISPUTE_OPENED,
        DISPUTE_RESOLVED,
        MESSAGE_RECEIVED,
        SYSTEM_ALERT
    }
}