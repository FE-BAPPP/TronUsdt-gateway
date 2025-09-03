package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.entity.HdMasterWallet;
import com.UsdtWallet.UsdtWallet.service.HdWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
@Slf4j
public class AdminWalletController {

    private final HdWalletService hdWalletService;

    /**
     * Get master wallet information and TRX balance
     */
    @GetMapping("/master")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMasterWalletInfo() {
        try {
            HdMasterWallet masterWallet = hdWalletService.getMasterWallet();
            BigDecimal trxBalance = hdWalletService.getTrxBalance(masterWallet.getMasterAddress());

            Map<String, Object> result = Map.of(
                "address", masterWallet.getMasterAddress(),
                "trxBalance", trxBalance,
                "createdAt", masterWallet.getCreatedAt(),
                "isLowBalance", trxBalance.compareTo(new BigDecimal("100")) < 0
            );

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error getting master wallet info", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get master wallet info: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Check master wallet TRX balance
     */
    @GetMapping("/master/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkMasterBalance() {
        try {
            hdWalletService.checkAndLogMasterWalletBalance();
            HdMasterWallet masterWallet = hdWalletService.getMasterWallet();
            BigDecimal balance = hdWalletService.getTrxBalance(masterWallet.getMasterAddress());

            Map<String, Object> result = Map.of(
                "address", masterWallet.getMasterAddress(),
                "balance", balance,
                "unit", "TRX",
                "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error checking master balance", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to check balance: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get wallet pool statistics
     */
    @GetMapping("/pool/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPoolStats() {
        try {
            HdWalletService.PoolStats stats = hdWalletService.getPoolStats();
            HdMasterWallet masterWallet = hdWalletService.getMasterWallet();
            BigDecimal masterBalance = hdWalletService.getTrxBalance(masterWallet.getMasterAddress());

            Map<String, Object> result = Map.of(
                "walletPool", Map.of(
                    "total", stats.total(),
                    "free", stats.free(),
                    "assigned", stats.assigned(),
                    "active", stats.active()
                ),
                "masterWallet", Map.of(
                    "address", masterWallet.getMasterAddress(),
                    "trxBalance", masterBalance,
                    "isLowBalance", masterBalance.compareTo(new BigDecimal("100")) < 0
                )
            );

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error getting pool stats", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get pool stats: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Generate additional child wallets
     */
    @PostMapping("/pool/generate")
    public ResponseEntity<ApiResponse<String>> generateWallets(@RequestParam(defaultValue = "500") int count) {
        try {
            if (count <= 0 || count > 2000) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>builder()
                        .success(false)
                        .message("Count must be between 1 and 2000")
                        .build());
            }

            hdWalletService.generateChildWallets(count);

            return ResponseEntity.ok(ApiResponse.success(
                String.format("Successfully generated %d child wallets", count)
            ));
        } catch (Exception e) {
            log.error("Error generating wallets", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to generate wallets: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Force regenerate all child wallets (DANGEROUS - clears all existing)
     */
    @PostMapping("/pool/force-regenerate")
    public ResponseEntity<ApiResponse<String>> forceRegenerateWallets(@RequestParam(defaultValue = "false") boolean confirm) {
        try {
            if (!confirm) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>builder()
                        .success(false)
                        .message("Must set confirm=true to force regenerate")
                        .build());
            }

            hdWalletService.forceRegenerateChildWallets();

            return ResponseEntity.ok(ApiResponse.success(
                "Successfully force regenerated all child wallets"
            ));
        } catch (Exception e) {
            log.error("Error force regenerating wallets", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to force regenerate: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Health check for wallet system
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        try {
            HdWalletService.PoolStats stats = hdWalletService.getPoolStats();
            HdMasterWallet masterWallet = hdWalletService.getMasterWallet();
            BigDecimal masterBalance = hdWalletService.getTrxBalance(masterWallet.getMasterAddress());

            boolean isHealthy = stats.free() > 100 && masterBalance.compareTo(new BigDecimal("50")) > 0;

            Map<String, Object> health = Map.of(
                "status", isHealthy ? "HEALTHY" : "WARNING",
                "freeWallets", stats.free(),
                "masterTrxBalance", masterBalance,
                "checks", Map.of(
                    "sufficientWallets", stats.free() > 100,
                    "sufficientTrx", masterBalance.compareTo(new BigDecimal("50")) > 0
                ),
                "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(ApiResponse.success(health));
        } catch (Exception e) {
            log.error("Health check failed", e);
            Map<String, Object> health = Map.of(
                "status", "ERROR",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.ok(ApiResponse.success(health));
        }
    }
}
