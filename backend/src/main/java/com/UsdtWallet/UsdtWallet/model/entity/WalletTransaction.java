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

@Entity
@Table(name = "wallet_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tx_hash", unique = true, nullable = false)
    private String txHash;

    @Column(name = "from_address", nullable = false)
    private String fromAddress;

    @Column(name = "to_address", nullable = false)
    private String toAddress;

    @Column(name = "amount", precision = 36, scale = 18, nullable = false)
    private BigDecimal amount;

    @Column(name = "token_address")
    private String tokenAddress; // USDT contract address

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "block_timestamp")
    private LocalDateTime blockTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private TransactionDirection direction;

    @Column(name = "user_id")
    private String userId; // User who owns the wallet

    @Column(name = "gas_used")
    private BigDecimal gasUsed;

    @Column(name = "gas_price")
    private BigDecimal gasPrice;

    @Column(name = "confirmation_count")
    private Integer confirmationCount;

    @Column(name = "is_swept", nullable = false)
    @Builder.Default
    private Boolean isSwept = false;

    @Column(name = "sweep_tx_hash")
    private String sweepTxHash;

    @Column(name = "swept_at")
    private LocalDateTime sweptAt;

    @Column(name = "points_credited")
    private BigDecimal pointsCredited;

    @Column(name = "points_credited_at")
    private LocalDateTime pointsCreditedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TransactionType {
        DEPOSIT,
        SWEEP,
        WITHDRAWAL,
        INTERNAL_TRANSFER
    }

    public enum TransactionStatus {
        PENDING,
        CONFIRMED,
        FAILED,
        PROCESSING,
        COMPLETED
    }

    public enum TransactionDirection {
        IN,
        OUT
    }
}
