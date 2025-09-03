package com.UsdtWallet.UsdtWallet.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "child_wallet_pool")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildWalletPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "derivation_index", nullable = false, unique = true)
    private Integer derivationIndex;

    @Column(name = "address", length = 64, nullable = false, unique = true)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WalletStatus status = WalletStatus.FREE;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "first_deposit_at")
    private LocalDateTime firstDepositAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum WalletStatus {
        FREE, ASSIGNED, ACTIVE, RETIRED
    }
}