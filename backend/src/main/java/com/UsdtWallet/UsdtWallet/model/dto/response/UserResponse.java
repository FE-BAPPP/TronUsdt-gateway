package com.UsdtWallet.UsdtWallet.model.dto.response;

import com.UsdtWallet.UsdtWallet.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String code;
    private String fullName;
    private User.Gender gender;
    private User.Role role;
    private String avatar;
    private String email;
    private String phone;
    private String salt;
    private int status;
    private boolean isAdmin;
    private boolean isUser;
    private boolean isActive;
    private String userCreated;
    private LocalDateTime dateCreated;
    private String userUpdated;
    private LocalDateTime dateUpdated;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.isActive())
                .dateCreated(user.getDateCreated())
                .dateUpdated(user.getDateUpdated())
                .build();
    }

}
