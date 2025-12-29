package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResponse {
    private UUID id;
    private UUID projectId;
    private String projectTitle;
    private UUID raisedBy;
    private String raisedByName;
    private String reason;
    private String evidence;
    private String status;
    private String adminNotes;
    private UUID resolvedBy;
    private String resolvedByName;
    private String resolution;
    private BigDecimal refundAmount;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}