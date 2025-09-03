package com.UsdtWallet.UsdtWallet.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositNotificationDto {
    private String txHash;
    private String toAddress;
    private String fromAddress;
    private BigDecimal amount;
    private String tokenAddress;
    private Long blockNumber;
    private LocalDateTime blockTimestamp;
    private Integer confirmations;
    private String userId;
    private String status;
}
