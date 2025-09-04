package com.UsdtWallet.UsdtWallet.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawalRequest {

    @NotBlank(message = "Destination address is required")
    @Pattern(regexp = "^T[A-Za-z1-9]{33}$", message = "Invalid TRON address format")
    private String toAddress;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Minimum withdrawal amount is 1 USDT")
    @DecimalMax(value = "100000.0", message = "Maximum withdrawal amount is 100,000 USDT")
    private BigDecimal amount;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;

    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid 2FA code format")
    private String twoFactorCode;

    @NotBlank(message = "Password is required for withdrawal")
    private String password;
}
