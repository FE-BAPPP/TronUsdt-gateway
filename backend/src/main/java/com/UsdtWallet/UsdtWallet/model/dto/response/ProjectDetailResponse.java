package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailResponse {
    private UUID id;
    private UUID jobId;
    private String jobTitle;
    
    // Employer info
    private UUID employerId;
    private String employerName;
    private String employerAvatar;
    
    // Freelancer info
    private UUID freelancerId;
    private String freelancerName;
    private String freelancerAvatar;
    private Double freelancerRating;
    
    // Project details
    private BigDecimal agreedAmount;
    private String currency;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    // Milestones
    private List<MilestoneResponse> milestones;
    private Integer totalMilestones;
    private Integer completedMilestones;
    
    // Financial
    private BigDecimal totalPaid;
    private BigDecimal remainingAmount;
    private BigDecimal escrowBalance;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}