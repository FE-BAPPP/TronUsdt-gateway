package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import com.UsdtWallet.UsdtWallet.repository.UserRepository;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.TwoFactorAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthController {

    private final UserRepository userRepository;
    private final TwoFactorAuthService twoFactorAuthService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setup(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("2FA is already enabled")
                        .build());
            }

            String secret = twoFactorAuthService.generateSecret();
            user.setTwoFactorTempSecret(secret);
            userRepository.save(user);

            String accountName = StringUtils.hasText(user.getEmail()) ? user.getEmail() : user.getUsername();
            String otpauthUrl = twoFactorAuthService.buildOtpAuthUrl(accountName, secret);

            Map<String, Object> data = Map.of(
                    "otpauthUrl", otpauthUrl,
                    "secret", secret
            );
            return ResponseEntity.ok(ApiResponse.success("2FA setup initiated", data));
        } catch (Exception e) {
            log.error("2FA setup failed", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("2FA setup failed: " + e.getMessage())
                    .build());
        }
    }

    public record EnableRequest(String code) {}

    @PostMapping("/enable")
    public ResponseEntity<ApiResponse<Map<String, Object>>> enable(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody EnableRequest req) {
        try {
            if (req == null || !StringUtils.hasText(req.code()) || !req.code().matches("^[0-9]{6}$")) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Invalid 2FA code")
                        .build());
            }

            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("2FA is already enabled")
                        .build());
            }

            String tempSecret = user.getTwoFactorTempSecret();
            if (!StringUtils.hasText(tempSecret)) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("No 2FA setup in progress")
                        .build());
            }

            boolean ok = false;
            try {
                ok = twoFactorAuthService.verifyCode(tempSecret, Integer.parseInt(req.code()));
            } catch (NumberFormatException ignored) {}

            if (!ok) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Incorrect 2FA code")
                        .build());
            }

            user.setTwoFactorEnabled(true);
            user.setTwoFactorSecret(tempSecret);
            user.setTwoFactorTempSecret(null);
            user.setTwoFactorEnabledAt(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "twoFactorEnabled", true
            )));
        } catch (Exception e) {
            log.error("Enable 2FA failed", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Enable 2FA failed: " + e.getMessage())
                    .build());
        }
    }

    public record DisableRequest(String password, String code) {}

    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<Map<String, Object>>> disable(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DisableRequest req) {
        try {
            if (req == null || !StringUtils.hasText(req.password())) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Password is required")
                        .build());
            }

            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!passwordEncoder.matches(req.password(), user.getPassword())) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Invalid password")
                        .build());
            }

            // If code is provided, verify; if not provided, proceed with password-only
            if (StringUtils.hasText(req.code())) {
                String secret = user.getTwoFactorSecret();
                if (!StringUtils.hasText(secret) || !twoFactorAuthService.verifyCode(secret, Integer.parseInt(req.code()))) {
                    return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Invalid 2FA code")
                            .build());
                }
            }

            user.setTwoFactorEnabled(false);
            user.setTwoFactorSecret(null);
            user.setTwoFactorTempSecret(null);
            user.setTwoFactorEnabledAt(null);
            userRepository.save(user);

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "twoFactorEnabled", false
            )));
        } catch (Exception e) {
            log.error("Disable 2FA failed", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Disable 2FA failed: " + e.getMessage())
                    .build());
        }
    }
}

