package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationResponse {
    private UUID userId;
    private String username;
    private String email;
    private String fullName;
    private String role; // 🆕 NEW: Return role
    private String walletAddress;
    private LocalDateTime registeredAt;
    private String message;
}
