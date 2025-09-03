package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.SweepResultDto;
import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.entity.WalletTransaction;
import com.UsdtWallet.UsdtWallet.repository.WalletTransactionRepository;
import com.UsdtWallet.UsdtWallet.service.UsdtSweepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/deposits")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class DepositController {

    private final WalletTransactionRepository walletTransactionRepository;
    private final UsdtSweepService usdtSweepService;

    /**
     * GET /api/admin/deposits/recent
     * Danh sách deposit mới detect (limit mặc định 50)
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecentDeposits(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 200)));
            var page = walletTransactionRepository.findByTransactionTypeOrderByCreatedAtDesc(
                WalletTransaction.TransactionType.DEPOSIT, pageable
            );

            List<WalletTransaction> deposits = page.getContent();

            Map<String, Object> result = Map.of(
                "deposits", deposits,
                "count", deposits.size()
            );

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error fetching recent deposits", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to fetch recent deposits: " + e.getMessage())
                    .build());
        }
    }

    /**
     * GET /api/admin/deposits/pending
     * Các deposit detect nhưng chưa sweep (isSwept=false)
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingUnsweptDeposits(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 200)));
            var page = walletTransactionRepository.findByTransactionTypeAndIsSweptFalseOrderByCreatedAtDesc(
                WalletTransaction.TransactionType.DEPOSIT, pageable
            );

            List<WalletTransaction> pending = page.getContent();

            Map<String, Object> result = Map.of(
                "deposits", pending,
                "count", pending.size()
            );

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error fetching pending deposits", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to fetch pending deposits: " + e.getMessage())
                    .build());
        }
    }

    /**
     * (Optional) POST /api/admin/deposits/sweep/{address}
     * Admin sweep thủ công 1 địa chỉ khi auto bị kẹt
     */
    @PostMapping("/sweep/{address}")
    public ResponseEntity<ApiResponse<SweepResultDto>> manualSweepAddress(@PathVariable String address) {
        try {
            log.info("Manual sweep triggered for address: {}", address);
            SweepResultDto result = usdtSweepService.sweepAddress(address);
            return ResponseEntity.ok(ApiResponse.success("Sweep executed", result));
        } catch (Exception e) {
            log.error("Error sweeping address {}", address, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<SweepResultDto>builder()
                    .success(false)
                    .message("Sweep failed: " + e.getMessage())
                    .build());
        }
    }
}
