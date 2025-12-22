package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerProfileResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private String avatar;
    private String professionalTitle;
    private String bio;
    private BigDecimal hourlyRate;
    private String availability;
    private String portfolioUrl;
    private String linkedinUrl;
    private String githubUrl;
    private BigDecimal totalEarnings;
    private Integer jobsCompleted;
    private Double avgRating;
    private Integer activeProjects;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}