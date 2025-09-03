package com.UsdtWallet.UsdtWallet.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SweepResultDto {
    private String masterWalletAddress;
    private Integer totalTransactions;
    private BigDecimal totalAmount;
    private List<SweepTransactionDto> successfulSweeps;
    private List<SweepTransactionDto> failedSweeps;
    private BigDecimal totalGasUsed;
    private String status;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SweepTransactionDto {
        private String fromAddress;
        private String txHash;
        private BigDecimal amount;
        private String status;
        private String errorMessage;
        private BigDecimal gasUsed;
    }
}
