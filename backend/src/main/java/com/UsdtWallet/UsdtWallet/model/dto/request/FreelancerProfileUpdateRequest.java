package com.UsdtWallet.UsdtWallet.model.dto.request;

import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerProfileUpdateRequest {
    private String professionalTitle;
    private String bio;
    
    @Min(value = 0, message = "Hourly rate cannot be negative")
    private BigDecimal hourlyRate;
    
    private String availability; // AVAILABLE, BUSY, UNAVAILABLE
    private String portfolioUrl;
    private String linkedinUrl;
    private String githubUrl;
}