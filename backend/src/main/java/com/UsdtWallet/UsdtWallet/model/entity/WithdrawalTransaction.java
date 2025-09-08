package com.UsdtWallet.UsdtWallet.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "withdrawal_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "to_address", nullable = false)
    private String toAddress;

    @Column(name = "amount", precision = 36, scale = 18, nullable = false)
    private BigDecimal amount;

    @Column(name = "fee", precision = 36, scale = 18, nullable = false)
    private BigDecimal fee;

    @Column(name = "net_amount", precision = 36, scale = 18, nullable = false)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "confirmations")
    private Integer confirmations = 0;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum WithdrawalStatus {
        CREATED,
        PENDING,
        PROCESSING,
        BROADCASTING,
        SENT,
        CONFIRMED,
        FAILED,
        CANCELLED
    }

    // Helper methods
    public boolean canBeCancelled() {
        // status PENDING, not processed yet, and no tx broadcast
        return status == WithdrawalStatus.PENDING
                && processedAt == null
                && (txHash == null || txHash.isEmpty());
    }

    public boolean canBeRetried() {
        return status == WithdrawalStatus.FAILED && retryCount < maxRetries;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public boolean isCompleted() {
        return status == WithdrawalStatus.CONFIRMED ||
               status == WithdrawalStatus.FAILED ||
               status == WithdrawalStatus.CANCELLED;
    }
}
