package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserRegistrationResponse {
    private UUID userId;
    private String username;
    private String email;
    private String fullName;
    private String walletAddress;
    private LocalDateTime registeredAt;
    private String message;
}
