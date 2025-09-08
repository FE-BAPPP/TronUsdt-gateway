package com.UsdtWallet.UsdtWallet.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class WithdrawalConfirmRequest {

    @NotNull(message = "withdrawalId is required")
    private Long withdrawalId;

    @NotBlank(message = "Password is required")
    private String password;

    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid 2FA code format")
    private String twoFactorCode;
}

