package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.entity.PointsLedger;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder; // added
import com.UsdtWallet.UsdtWallet.repository.UserRepository; // added
import org.springframework.security.access.prepost.PreAuthorize; // added

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('FREELANCER') or hasRole('EMPLOYER')") // ✅ ADD THIS
public class PointsController {

    private final PointsService pointsService;
    private final UserRepository userRepository; // added
    private final PasswordEncoder passwordEncoder; // added

    /**
     * Get current user's points balance (includes available and locked)
     */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            java.util.UUID userId = userPrincipal.getId();
            java.math.BigDecimal balance = pointsService.getCurrentBalance(userId);
            java.math.BigDecimal available = pointsService.getAvailableBalance(userId);
            java.math.BigDecimal locked = balance.subtract(available).max(java.math.BigDecimal.ZERO);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "userId", userId,
                    "balance", balance,
                    "available", available,
                    "locked", locked,
                    "currency", "POINTS"
                )
            ));

        } catch (Exception e) {
            log.error("Error getting balance for user: {}", userPrincipal.getId(), e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Failed to get balance"
            ));
        }
    }

    /**
     * Get user's transaction history
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getTransactionHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "50") int limit) {

        try {
            java.util.UUID userId = userPrincipal.getId();
            List<PointsLedger> history = pointsService.getTransactionHistory(userId, limit);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", history
            ));

        } catch (Exception e) {
            log.error("Error getting transaction history for user: {}", userPrincipal.getId(), e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Failed to get transaction history"
            ));
        }
    }

    /**
     * 📊 Lấy thống kê tổng quan của user
     * - Số dư hiện tại
     * - Tổng nạp
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            java.util.UUID userId = userPrincipal.getId();
            Map<String, Object> stats = pointsService.getUserStats(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            ));

        } catch (Exception e) {
            log.error("Error getting stats for user: {}", userPrincipal.getId(), e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Failed to get user statistics"
            ));
        }
    }

}
