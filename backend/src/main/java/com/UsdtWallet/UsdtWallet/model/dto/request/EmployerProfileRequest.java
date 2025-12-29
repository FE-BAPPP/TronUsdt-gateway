package com.UsdtWallet.UsdtWallet.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployerProfileRequest {
    
    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String companyName;
    
    @Size(max = 255, message = "Company website must not exceed 255 characters")
    private String companyWebsite;
    
    @Size(max = 255, message = "Company size must not exceed 255 characters")
    private String companySize; // e.g., "1-10", "11-50", "51-200", "201-500", "500+"
    
    @Size(max = 255, message = "Industry must not exceed 255 characters")
    private String industry; // e.g., "Technology", "Finance", "Healthcare", etc.
}
