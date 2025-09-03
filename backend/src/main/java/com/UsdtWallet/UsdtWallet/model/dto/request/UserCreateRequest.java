package com.UsdtWallet.UsdtWallet.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {

    private String username;
    private String password;
    private String email;
    private String phone;
    private String fullName;
}
