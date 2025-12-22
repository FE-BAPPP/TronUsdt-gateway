package com.UsdtWallet.UsdtWallet.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalCreateRequest {

    @NotNull(message = "Job ID is required")
    private UUID jobId;

    @NotBlank(message = "Cover letter is required")
    private String coverLetter;

    @NotNull(message = "Proposed amount is required")
    @Positive(message = "Proposed amount must be positive")
    private BigDecimal proposedAmount;

    @Positive(message = "Duration must be positive")
    private Integer estimatedDurationDays;
}