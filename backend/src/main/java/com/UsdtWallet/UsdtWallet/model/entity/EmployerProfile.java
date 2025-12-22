// File: backend/src/main/java/com/UsdtWallet/UsdtWallet/model/entity/EmployerProfile.java

package com.UsdtWallet.UsdtWallet.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employer_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class EmployerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private String companyName;
    private String companyWebsite;
    private String companySize; // SOLO, SMALL, MEDIUM, LARGE
    private String industry;

    @Builder.Default
    private Integer jobsPosted = 0;

    @Builder.Default
    private Integer activeProjects = 0;

    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}