package com.UsdtWallet.UsdtWallet.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "points_ledger")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, updatable = false, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "transaction_id")
    private String transactionId; // Reference to WalletTransaction

    @Column(name = "reference_id")
    private String referenceId; // For P2P transfers

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private PointsTransactionType transactionType;

    @Column(name = "amount", precision = 36, scale = 18, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 36, scale = 18, nullable = false)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 36, scale = 18, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "from_user_id")
    private String fromUserId; // For P2P transfers

    @Column(name = "to_user_id")
    private String toUserId; // For P2P transfers

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PointsTransactionStatus status;

    @Column(name = "usdt_amount", precision = 36, scale = 18)
    private BigDecimal usdtAmount; // Original USDT amount for deposits

    @Column(name = "exchange_rate", precision = 18, scale = 8)
    private BigDecimal exchangeRate; // USDT to Points rate

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum PointsTransactionType {
        DEPOSIT_CREDIT,    // USDT deposit → Points
        P2P_SEND,         // Send points to another user
        P2P_RECEIVE,      // Receive points from another user
        WITHDRAWAL_DEBIT, // Points → USDT withdrawal
        ADJUSTMENT,       // Manual adjustment by admin
        BONUS,           // Promotional bonus
        REFUND           // Refund transaction
    }

    public enum PointsTransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
