package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.entity.WithdrawalTransaction;
import com.UsdtWallet.UsdtWallet.repository.WithdrawalTransactionRepository;
import com.UsdtWallet.UsdtWallet.service.SystemMonitoringService;
import com.UsdtWallet.UsdtWallet.service.WithdrawalQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/withdrawals")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminWithdrawalsController {

    private final WithdrawalTransactionRepository withdrawalRepository;
    private final WithdrawalQueueService withdrawalQueueService;
    private final SystemMonitoringService systemMonitoringService;

    /**
     * GET /api/admin/withdrawals/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecentWithdrawals(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 200)));
            var page = withdrawalRepository.findAllByOrderByCreatedAtDesc(pageable);

            Map<String, Object> result = Map.of(
                "withdrawals", page.getContent(),
                "count", page.getNumberOfElements()
            );

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error fetching recent withdrawals", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to fetch recent withdrawals: " + e.getMessage())
                    .build());
        }
    }

    /**
     * GET /api/admin/withdrawals/failed
     */
    @GetMapping("/failed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFailedWithdrawals(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 200)));
            var page = withdrawalRepository.findByStatusOrderByUpdatedAtDesc(
                WithdrawalTransaction.WithdrawalStatus.FAILED, pageable
            );

            Map<String, Object> result = Map.of(
                "withdrawals", page.getContent(),
                "count", page.getNumberOfElements()
            );

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error fetching failed withdrawals", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to fetch failed withdrawals: " + e.getMessage())
                    .build());
        }
    }

    /**
     * POST /api/admin/withdrawals/retry/{id}
     */
    @PostMapping("/retry/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retryWithdrawal(@PathVariable Long id) {
        try {
            var withdrawal = withdrawalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found"));

            if (!withdrawal.canBeRetried()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Withdrawal cannot be retried in current status or max retries reached")
                        .build());
            }

            withdrawal.setStatus(WithdrawalTransaction.WithdrawalStatus.PENDING);
            withdrawalRepository.save(withdrawal);

            withdrawalQueueService.addToQueue(id);

            Map<String, Object> result = Map.of(
                "withdrawalId", id,
                "message", "Retry triggered",
                "status", withdrawal.getStatus()
            );

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error retrying withdrawal {}", id, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to retry: " + e.getMessage())
                    .build());
        }
    }

    // Shims under required path (already implemented in AdminDashboardController)
    @PostMapping("/emergency-stop")
    public ResponseEntity<ApiResponse<Map<String, Object>>> emergencyStop() {
        systemMonitoringService.emergencyStopWithdrawals();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "message", "Emergency stop activated"
        )));
    }

    @PostMapping("/resume")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resume() {
        systemMonitoringService.resumeWithdrawals();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "message", "Withdrawal processing resumed"
        )));
    }
}

