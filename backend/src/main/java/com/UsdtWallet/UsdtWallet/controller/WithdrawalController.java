package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.request.WithdrawalConfirmRequest;
import com.UsdtWallet.UsdtWallet.model.dto.request.WithdrawalRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.entity.WithdrawalTransaction;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/withdrawal")
@RequiredArgsConstructor
@Slf4j
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createWithdrawal(
            @Valid @RequestBody com.UsdtWallet.UsdtWallet.model.dto.request.WithdrawalCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            log.info("Automated withdrawal request from user: {}, amount: {}, address: {}",
                userPrincipal.getId(), request.getAmount(), request.getToAddress());

            WithdrawalTransaction withdrawal = withdrawalService.createWithdrawal(
                userPrincipal.getId(), request);

            Map<String, Object> result = Map.of(
                "withdrawalId", withdrawal.getId(),
                "amount", withdrawal.getAmount(),
                "toAddress", withdrawal.getToAddress(),
                "status", withdrawal.getStatus(),
                "fee", withdrawal.getFee(),
                "netAmount", withdrawal.getNetAmount(),
                "estimatedTime", "3-8 minutes (automated blockchain transfer)",
                "message", "Withdrawal is being processed automatically",
                "createdAt", withdrawal.getCreatedAt()
            );

            return ResponseEntity.ok(ApiResponse.success("Automated withdrawal initiated successfully", result));

        } catch (Exception e) {
            log.error("Error creating withdrawal request", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to create withdrawal: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get withdrawal history
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWithdrawalHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Map<String, Object> history = withdrawalService.getUserWithdrawalHistory(
                userPrincipal.getId(), page, size);

            return ResponseEntity.ok(ApiResponse.success(history));

        } catch (Exception e) {
            log.error("Error getting withdrawal history", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get withdrawal history: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get withdrawal status
     */
    @GetMapping("/status/{withdrawalId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWithdrawalStatus(
            @PathVariable Long withdrawalId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Map<String, Object> status = withdrawalService.getWithdrawalStatus(
                withdrawalId, userPrincipal.getId());

            return ResponseEntity.ok(ApiResponse.success(status));

        } catch (Exception e) {
            log.error("Error getting withdrawal status", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get withdrawal status: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Cancel pending withdrawal
     */
    @PostMapping("/cancel/{withdrawalId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelWithdrawal(
            @PathVariable Long withdrawalId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            boolean cancelled = withdrawalService.cancelWithdrawal(withdrawalId, userPrincipal.getId());

            Map<String, Object> result = Map.of(
                "withdrawalId", withdrawalId,
                "cancelled", cancelled,
                "message", cancelled ? "Withdrawal cancelled successfully" : "Cannot cancel withdrawal"
            );

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("Error cancelling withdrawal", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to cancel withdrawal: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get withdrawal limits and fees
     */
    @GetMapping("/limits")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWithdrawalLimits(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Map<String, Object> limits = withdrawalService.getWithdrawalLimits(userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success(limits));

        } catch (Exception e) {
            log.error("Error getting withdrawal limits", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get withdrawal limits: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Confirm withdrawal
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmWithdrawal(
            @Valid @RequestBody WithdrawalConfirmRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Map<String, Object> result = withdrawalService.confirmWithdrawal(userPrincipal.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("Withdrawal confirmed", result));
        } catch (Exception e) {
            log.error("Error confirming withdrawal", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to confirm withdrawal: " + e.getMessage())
                    .build());
        }
    }
}
