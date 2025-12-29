package com.UsdtWallet.UsdtWallet.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneRejectRequest {
    
    @NotBlank(message = "Reason is required")
    private String reason;
}