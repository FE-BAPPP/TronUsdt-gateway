// ProposalResponse.java
package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalResponse {
    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private UUID freelancerId;
    private String freelancerName;
    private String freelancerAvatar;
    private Double freelancerRating;
    private Integer freelancerCompletedJobs;
    private String coverLetter;
    private BigDecimal proposedAmount;
    private Integer estimatedDurationDays;
    private String status;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}