package com.UsdtWallet.UsdtWallet.model.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneUpdateRequest {
    
    private String title;
    private String description;
    
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private LocalDateTime dueDate;
}