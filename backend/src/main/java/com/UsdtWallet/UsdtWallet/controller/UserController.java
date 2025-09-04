package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.request.UserRegistrationRequest;
import com.UsdtWallet.UsdtWallet.model.dto.request.LoginRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.dto.response.UserRegistrationResponse;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.UsdtWallet.UsdtWallet.service.AuthTokenService;
import com.UsdtWallet.UsdtWallet.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import com.UsdtWallet.UsdtWallet.service.EmailService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final AuthTokenService authTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${spring.security.jwt.expiration:86400000}")
    private Long jwtExpirationInMs; // added

    /**
     * User registration with auto wallet assignment
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        try {
            log.info("=== STARTING USER REGISTRATION ===");
            log.info("Request received for username: {}, email: {}", request.getUsername(), request.getEmail());

            // Validate request
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                log.error("Username is null or empty");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<UserRegistrationResponse>builder()
                        .success(false)
                        .message("Username is required")
                        .build());
            }

            UserRegistrationResponse response = userService.registerUser(request);

            log.info("=== USER REGISTRATION SUCCESSFUL ===");
            log.info("User {} registered successfully with wallet {}",
                response.getUsername(), response.getWalletAddress());

            ApiResponse<UserRegistrationResponse> apiResponse = ApiResponse.success(
                "User registered successfully with auto-assigned wallet", response);

            log.info("Returning response: {}", apiResponse);
            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("=== USER REGISTRATION FAILED ===");
            log.error("Error details: ", e);

            ApiResponse<UserRegistrationResponse> errorResponse = ApiResponse.<UserRegistrationResponse>builder()
                .success(false)
                .message("Registration failed: " + e.getMessage())
                .data(null)
                .build();

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get user wallet address
     */
    @GetMapping("/wallet/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserWallet(
            @PathVariable String userId) {
        try {
            java.util.UUID userUuid = java.util.UUID.fromString(userId);
            String walletAddress = userService.getUserWalletAddress(userUuid);

            if (walletAddress != null) {
                Map<String, Object> result = Map.of(
                    "userId", userId,
                    "walletAddress", walletAddress
                );
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("No wallet assigned to user")
                        .build());
            }

        } catch (Exception e) {
            log.error("Error getting user wallet: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get user wallet: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get user info with wallet address
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserInfo(
            @PathVariable String username) {
        try {
            log.info("Getting user info for username: {}", username);

            User user = userService.getUserByUsername(username);
            String walletAddress = userService.getUserWalletAddress(user.getId());

            Map<String, Object> result = Map.of(
                "userId", user.getId().toString(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "walletAddress", walletAddress != null ? walletAddress : "No wallet assigned",
                "status", user.getStatus(),
                "createdAt", user.getDateCreated()
            );

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("Error getting user info: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get user info: " + e.getMessage())
                    .build());
        }
    }

    /**
     * User login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            log.info("Login attempt for username: {}", request.getUsername());

            Map<String, Object> response = userService.login(request.getUsername(), request.getPassword());

            log.info("Login successful for user: {}", request.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));

        } catch (Exception e) {
            log.error("Login failed for username: {}", request.getUsername(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Login failed: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Map<String, Object> userInfo = userService.getUserInfo(userPrincipal.getId().toString());
            return ResponseEntity.ok(ApiResponse.success(userInfo));
        } catch (Exception e) {
            log.error("Error getting user profile", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get profile: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get user wallet address and balance info
     */
    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserWallet(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            String walletAddress = userService.getUserWalletAddress(userPrincipal.getId());

            if (walletAddress != null) {
                Map<String, Object> result = userService.getUserWalletInfo(userPrincipal.getId());
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("No wallet assigned to user")
                        .build());
            }

        } catch (Exception e) {
            log.error("Error getting user wallet: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get wallet info: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get user deposit address for QR code
     */
    @GetMapping("/deposit-address")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDepositAddress(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            String walletAddress = userService.getUserWalletAddress(userPrincipal.getId());

            Map<String, Object> result = Map.of(
                "userId", userPrincipal.getId().toString(),
                "depositAddress", walletAddress,
                "network", "TRC20",
                "token", "USDT",
                "note", "Only send USDT (TRC20) to this address"
            );

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("Error getting deposit address: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get deposit address: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get user transaction history
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserTransactions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Map<String, Object> transactions = userService.getUserTransactions(
                userPrincipal.getId(), page, size);

            return ResponseEntity.ok(ApiResponse.success(transactions));

        } catch (Exception e) {
            log.error("Error getting user transactions: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get transactions: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Create admin account (one-time setup endpoint)
     */
    @PostMapping("/create-admin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAdmin(
            @RequestBody CreateAdminRequest request) {
        try {
            log.info("=== CREATE ADMIN REQUEST ===");
            log.info("Username: {}", request.getUsername());
            log.info("Email: {}", request.getEmail());
            log.info("Full Name: {}", request.getFullName());
            log.info("Password length: {}", request.getPassword() != null ? request.getPassword().length() : "null");

            // Validate request
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                log.error("Username is null or empty");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Username is required")
                        .build());
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                log.error("Password is null or empty");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Password is required")
                        .build());
            }

            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                log.error("Email is null or empty");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Email is required")
                        .build());
            }

            Map<String, Object> result = userService.createAdminAccount(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getFullName()
            );

            log.info("Admin account created via API: {}", request.getUsername());

            return ResponseEntity.ok(ApiResponse.success("Admin account created successfully", result));

        } catch (Exception e) {
            log.error("Failed to create admin account - Exception type: {}", e.getClass().getSimpleName());
            log.error("Failed to create admin account - Message: {}", e.getMessage());
            log.error("Failed to create admin account - Stack trace: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to create admin account: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"))
                    .build());
        }
    }

    /**
     * User logout: blacklist current JWT until expiry
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(HttpServletRequest request) {
        try {
            String bearer = request.getHeader("Authorization");
            if (!StringUtils.hasText(bearer) || !bearer.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String,Object>>builder().success(false).message("Missing token").build());
            }
            String token = bearer.substring(7);
            authTokenService.blacklistToken(token, jwtTokenProvider.getExpirationTime());
            return ResponseEntity.ok(ApiResponse.success(Map.of("loggedOut", true)));
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String,Object>>builder().success(false).message("Logout failed: " + e.getMessage()).build());
        }
    }

    /**
     * Forgot password: issue reset token (dev: return token in response)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPassword(@RequestBody Map<String, String> body,
                                                                           HttpServletRequest request) {
        try {
            String email = body.get("email");
            if (!StringUtils.hasText(email)) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String,Object>>builder().success(false).message("Email is required").build());
            }

            // Throttle per email and IP
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            if (!StringUtils.hasText(ip)) {
                ip = request.getRemoteAddr();
            }
            boolean allowed = authTokenService.acquireResetThrottle(email, ip);
            if (!allowed) {
                // Always generic response (non-enumerable)
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "requested", true,
                    "message", "If the email exists, we've sent reset instructions"
                )));
            }

            // Avoid user enumeration
            boolean exists = false;
            try { exists = userService.existsByEmail(email); } catch (Exception ignored) {}

            if (exists) {
                try {
                    User user = userService.getUserByEmail(email);
                    String token = authTokenService.createPasswordResetTokenReplacingPrevious(
                        user.getId().toString(), java.time.Duration.ofMinutes(15));
                    emailService.sendPasswordResetEmail(email, token);
                } catch (Exception sendEx) {
                    log.warn("Failed to process password reset for {}: {}", email, sendEx.getMessage());
                }
            } else {
                try { Thread.sleep(120); } catch (InterruptedException ignored) {}
            }

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "requested", true,
                "message", "If the email exists, we've sent reset instructions"
            )));
        } catch (Exception e) {
            log.error("Forgot password failed", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String,Object>>builder().success(false).message("Forgot password failed").build());
        }
    }

    /**
     * Reset password with token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            String newPassword = body.get("newPassword");
            if (!StringUtils.hasText(token) || !StringUtils.hasText(newPassword)) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String,Object>>builder().success(false).message("Token and newPassword are required").build());
            }
            String userId = authTokenService.validateResetToken(token);
            if (!StringUtils.hasText(userId)) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String,Object>>builder().success(false).message("Invalid or expired token").build());
            }
            boolean updated = userService.updatePassword(java.util.UUID.fromString(userId), passwordEncoder.encode(newPassword));
            if (updated) {
                authTokenService.consumeResetToken(token);
                return ResponseEntity.ok(ApiResponse.success(Map.of("reset", true)));
            }
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String,Object>>builder().success(false).message("Failed to reset password").build());
        } catch (Exception e) {
            log.error("Reset password failed", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String,Object>>builder().success(false).message("Reset password failed: " + e.getMessage()).build());
        }
    }

    /**
     * Update profile fields
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UpdateProfileRequest req) {
        try {
            Map<String, Object> updated = userService.updateProfile(userPrincipal.getId(), req);
            return ResponseEntity.ok(ApiResponse.success("Profile updated", updated));
        } catch (Exception e) {
            log.error("Update profile failed", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String,Object>>builder().success(false).message("Update profile failed: " + e.getMessage()).build());
        }
    }

    public static class UpdateProfileRequest {
        public String fullName;
        public String phone;
        public String avatar;
        public String description;
        public String email;
    }

    // DTO class for admin creation request
    public static class CreateAdminRequest {
        private String username;
        private String password;
        private String email;
        private String fullName;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
    }
}
