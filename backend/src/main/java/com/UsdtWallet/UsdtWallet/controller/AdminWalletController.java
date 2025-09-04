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
}
