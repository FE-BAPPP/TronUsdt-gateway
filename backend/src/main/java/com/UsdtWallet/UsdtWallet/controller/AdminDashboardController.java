package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final HdWalletService hdWalletService;
    private final WithdrawalQueueService withdrawalQueueService;
    private final WithdrawalProcessorService withdrawalProcessorService;
    private final DepositScannerService depositScannerService;
    private final TronApiService tronApiService;
    private final SystemMonitoringService systemMonitoringService;

    /**
     * dashboard overview
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardOverview() {
        try {
            // Master wallet info
            var masterWallet = hdWalletService.getMasterWallet();
            var masterBalance = hdWalletService.getTrxBalance(masterWallet.getMasterAddress());
            var masterUsdtBalance = tronApiService.getUsdtBalance(masterWallet.getMasterAddress());

            // Wallet pool stats
            var poolStats = hdWalletService.getPoolStats();

            // Withdrawal stats
            var withdrawalStats = withdrawalProcessorService.getProcessingStats();
            var queueStats = withdrawalQueueService.getQueueStats();

            // Deposit scanner stats
            var scannerStats = depositScannerService.getScanningStats();

            // System health
            var systemHealth = systemMonitoringService.getSystemHealth();

            Map<String, Object> overview = Map.of(
                "masterWallet", Map.of(
                    "address", masterWallet.getMasterAddress(),
                    "trxBalance", masterBalance,
                    "usdtBalance", masterUsdtBalance,
                    "isLowTrxBalance", masterBalance.compareTo(new java.math.BigDecimal("100")) < 0,
                    "isLowUsdtBalance", masterUsdtBalance.compareTo(new java.math.BigDecimal("1000")) < 0
                ),
                "walletPool", Map.of(
                    "total", poolStats.total(),
                    "free", poolStats.free(),
                    "assigned", poolStats.assigned(),
                    "active", poolStats.active(),
                    "utilizationRate", poolStats.total() > 0 ?
                        (double)(poolStats.assigned() + poolStats.active()) / poolStats.total() * 100 : 0
                ),
                "withdrawals", withdrawalStats,
                "withdrawalQueue", queueStats,
                "depositScanner", scannerStats,
                "systemHealth", systemHealth,
                "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(ApiResponse.success(overview));

        } catch (Exception e) {
            log.error("Error getting dashboard overview", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get dashboard overview: " + e.getMessage())
                    .build());
        }
    }

    /**
     * system monitoring data
     */
    @GetMapping("/monitoring")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemMonitoring() {
        try {
            Map<String, Object> monitoring = systemMonitoringService.getDetailedSystemStatus();
            return ResponseEntity.ok(ApiResponse.success(monitoring));

        } catch (Exception e) {
            log.error("Error getting system monitoring data", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get monitoring data: " + e.getMessage())
                    .build());
        }
    }

    /**
     * reset block scanner
     */
    @PostMapping("/deposit/scan/reset")
    public ResponseEntity<Map<String, Object>> resetScanPosition() {
        try {
            Long currentBlock = tronApiService.getLatestBlockNumber();
            if (currentBlock == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Failed to get current block number"
                ));
            }

            // Reset to current block - 50 for fresh scanning
            Long newScanPosition = currentBlock - 50;
            depositScannerService.resetScanPosition(newScanPosition);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Scan position reset successfully",
                    "data", Map.of(
                            "currentBlock", currentBlock,
                            "newScanPosition", newScanPosition,
                            "blocksToScan", 50,
                            "reason", "Reset for fresh scanning from recent blocks"
                    )
            ));

        } catch (Exception e) {
            log.error("Error resetting scan position", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to reset scan position: " + e.getMessage()
            ));
        }
    }
}
