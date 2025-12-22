// File: backend/src/main/java/com/UsdtWallet/UsdtWallet/model/entity/FreelancerProfile.java

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
@Table(name = "freelancer_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FreelancerProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // ✅ FIX: Use @OneToOne relationship instead of userId field
    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "professional_title", length = 255)
    private String professionalTitle;
    
    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "hourly_rate", precision = 38, scale = 2)
    private BigDecimal hourlyRate;

    @Column(length = 255)
    @Builder.Default
    private String availability = "AVAILABLE"; // AVAILABLE, BUSY, NOT_AVAILABLE

    @Column(name = "total_earnings", precision = 38, scale = 2)
    @Builder.Default
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "jobs_completed")
    @Builder.Default
    private Integer jobsCompleted = 0;

    // ✅ FIX: Change from Double to BigDecimal to match DB schema
    @Column(name = "avg_rating", precision = 38, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "portfolio_url", length = 255)
    private String portfolioUrl;
    
    @Column(name = "github_url", length = 255)
    private String githubUrl;
    
    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}