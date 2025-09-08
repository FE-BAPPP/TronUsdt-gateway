package com.UsdtWallet.UsdtWallet.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String username;

    private String code;

    private String fullName;

    private String birthday;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String avatar;

    private String description;

    private String address;

    private String email;

    private String phone;

    private String password;

    private String salt;

    private int status;

    private boolean isAdmin;

    private boolean isUser;
    private boolean isActive;

    private String secretKey;

    private String deviceId;

    private String userCreated;
    @CreatedDate
    private LocalDateTime  dateCreated;

    private String userUpdated;

    @LastModifiedDate
    private LocalDateTime dateUpdated;

    // Security timestamps
    private LocalDateTime passwordChangedAt;
    private LocalDateTime withdrawalsDisabledUntil;

    // 2FA (Google Authenticator)
    @Column(name = "two_factor_enabled")
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    @Column(name = "two_factor_temp_secret")
    private String twoFactorTempSecret;

    @Column(name = "two_factor_enabled_at")
    private LocalDateTime twoFactorEnabledAt;

    @PrePersist
    @PreUpdate
    private void ensureDefaults() {
        if (twoFactorEnabled == null) twoFactorEnabled = false;
    }

    // Backward-compatible accessor for code using boolean-style getter
    public boolean isTwoFactorEnabled() {
        return Boolean.TRUE.equals(this.twoFactorEnabled);
    }

    public enum Gender {
        MALE, FEMALE, UNKNOWN;
    }

    public enum Role {
        USER, ADMIN;
    }
}
