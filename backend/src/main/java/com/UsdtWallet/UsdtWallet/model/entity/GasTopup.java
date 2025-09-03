package com.UsdtWallet.UsdtWallet.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gas_topups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GasTopup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "child_index", nullable = false)
    private Integer childIndex;

    @Column(name = "amount_trx", precision = 30, scale = 6, nullable = false)
    private BigDecimal amountTrx;

    @Column(name = "tx_hash", length = 128)
    private String txHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TopupStatus status = TopupStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum TopupStatus {
        PENDING, SENT, CONFIRMED, FAILED
    }
}