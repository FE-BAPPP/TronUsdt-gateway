package com.UsdtWallet.UsdtWallet.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResolveRequest {
    
    @NotBlank(message = "Resolution is required")
    private String resolution;
    
    private BigDecimal refundAmount;
    
    private String adminNotes;
}