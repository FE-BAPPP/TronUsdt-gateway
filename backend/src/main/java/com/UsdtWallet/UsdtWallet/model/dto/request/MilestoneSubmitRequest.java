package com.UsdtWallet.UsdtWallet.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneSubmitRequest {
    
    @NotBlank(message = "Deliverables are required")
    private String deliverables;
    
    private String notes;
}