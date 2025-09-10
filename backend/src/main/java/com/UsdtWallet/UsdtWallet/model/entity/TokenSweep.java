package com.UsdtWallet.UsdtWallet.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_sweeps")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenSweep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "child_index", nullable = false)
    private Integer childIndex;

    @Column(name = "child_address", length = 64, nullable = false)
    private String childAddress;

    @Column(name = "child_wallet_id")
    private Long childWalletId;

    @Column(name = "master_address", length = 64, nullable = false)
    private String masterAddress;

    @Column(name = "amount", precision = 36, scale = 6, nullable = false)
    private BigDecimal amount;

    @Column(name = "sweep_tx_hash", length = 128)
    private String sweepTxHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SweepStatus status = SweepStatus.PENDING;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SweepStatus {
        PENDING,     // Đã tạo record, chưa broadcast
        SENT,        // Đã broadcast, chờ confirm
        CONFIRMED,   // Transaction confirmed trên blockchain
        FAILED       // Có lỗi xảy ra
    }
}