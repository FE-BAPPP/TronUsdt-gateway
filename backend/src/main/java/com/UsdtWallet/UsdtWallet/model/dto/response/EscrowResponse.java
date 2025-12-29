package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO cho Escrow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscrowResponse {

    private UUID id;
    private UUID projectId;
    private UUID milestoneId;
    private String milestoneTitle;
    private UUID employerId;
    private UUID freelancerId;
    
    private BigDecimal amount;
    private BigDecimal platformFee;
    private String currency;
    
    private String status; // LOCKED, RELEASED, REFUNDED, DISPUTED
    
    private LocalDateTime lockedAt;
    private LocalDateTime releasedAt;
    private LocalDateTime refundedAt;
    private UUID releasedTo;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
