package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private UUID id;
    private UUID employerId;
    private String employerName;
    private String title;
    private String description;
    
    private String type; 
    
    // ✅ ADD: Main budget
    private BigDecimal budget;
    
    // Optional budget range
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    
    private String currency;
    
    // ✅ ADD: Duration
    private String duration;
    
    private LocalDate deadline;
    private String status;
    
    private Set<String> skills;
    private Integer proposalCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}