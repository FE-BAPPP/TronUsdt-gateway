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

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Slf4j
public class PointsController {

    private final PointsService pointsService;
    private final UserRepository userRepository; // added
    private final PasswordEncoder passwordEncoder; // added

    /**
     * Get current user's points balance
     */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            String userId = userPrincipal.getId().toString(); // Convert UUID to String
            BigDecimal balance = pointsService.getCurrentBalance(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "userId", userId,
                    "balance", balance,
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
            String userId = userPrincipal.getId().toString(); // Convert UUID to String
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
     * Get user's P2P transaction history
     */
    @GetMapping("/p2p-history")
    public ResponseEntity<Map<String, Object>> getP2PHistory(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            String userId = userPrincipal.getId().toString(); // Convert UUID to String
            List<PointsLedger> p2pHistory = pointsService.getP2PHistory(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", p2pHistory
            ));

        } catch (Exception e) {
            log.error("Error getting P2P history for user: {}", userPrincipal.getId(), e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Failed to get P2P history"
            ));
        }
    }

    /**
     * Get user statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            String userId = userPrincipal.getId().toString(); // Convert UUID to String
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

    /**
     * Transfer points to another user (P2P)
     */
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transferPoints(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody TransferRequest request) {

        try {
            String fromUserId = userPrincipal.getId().toString(); // Convert UUID to String

            // Verify password like Binance
            var user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Invalid password"
                ));
            }

            // Validate sufficient balance
            if (!pointsService.hasSufficientBalance(fromUserId, request.getAmount())) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Insufficient balance"
                ));
            }

            // Prevent self-transfer
            if (fromUserId.equals(request.getToUserId())) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Cannot transfer to yourself"
                ));
            }

            boolean success = pointsService.transferPoints(
                fromUserId,
                request.getToUserId(),
                request.getAmount(),
                request.getDescription()
            );

            if (success) {
                log.info("P2P transfer successful: {} points from {} to {}",
                    request.getAmount(), fromUserId, request.getToUserId());

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Transfer completed successfully",
                    "data", Map.of(
                        "amount", request.getAmount(),
                        "toUserId", request.getToUserId(),
                        "description", request.getDescription()
                    )
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Transfer failed"
                ));
            }

        } catch (Exception e) {
            log.error("Error in P2P transfer", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Transfer failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Request class for P2P transfer
     */
    public static class TransferRequest {
        @NotBlank(message = "Recipient user ID is required")
        private String toUserId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Minimum transfer amount is 0.01")
        private BigDecimal amount;

        private String description;

        @NotBlank(message = "Password is required")
        private String password; // added for Binance-like confirmation

        // Getters and setters
        public String getToUserId() { return toUserId; }
        public void setToUserId(String toUserId) { this.toUserId = toUserId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
