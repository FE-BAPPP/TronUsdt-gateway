package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthenResponse {
    private String AccessToken;
    private String RefreshToken;
    private String TokenType;
    private Long ExpiresIn;
    private UserResponse user;

}
