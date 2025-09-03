package com.UsdtWallet.UsdtWallet.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "hd_master_wallet")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HdMasterWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encrypted_mnemonic", nullable = false)
    private String encryptedMnemonic;

    @Column(name = "master_address", length = 64)
    private String masterAddress;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}