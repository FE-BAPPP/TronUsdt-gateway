package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO cho Audit Log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private UUID userId;
    private String action;
    private String entityType;
    private String entityId;
    private String details;
    private Boolean success;
    private String errorMessage;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
}
