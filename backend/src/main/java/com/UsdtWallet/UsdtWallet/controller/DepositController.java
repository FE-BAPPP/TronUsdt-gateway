package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.SweepResultDto;
import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.entity.WalletTransaction;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import com.UsdtWallet.UsdtWallet.repository.WalletTransactionRepository;
import com.UsdtWallet.UsdtWallet.repository.UserRepository;
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
    private final UserRepository userRepository;

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

            var enriched = page.getContent().stream().map(d -> {
                var m = new java.util.HashMap<String, Object>();
                m.put("id", d.getId());
                m.put("userId", d.getUserId());
                try {
                    java.util.Optional<User> u = userRepository.findById(d.getUserId());
                    m.put("username", u.map(User::getUsername).orElse(null));
                } catch (Exception ex) {
                    m.put("username", null);
                }
                m.put("amount", d.getAmount());
                m.put("fromAddress", d.getFromAddress());
                m.put("toAddress", d.getToAddress());
                m.put("txHash", d.getTxHash());
                m.put("createdAt", d.getCreatedAt());
                m.put("status", d.getStatus());
                return m;
            }).toList();

            Map<String, Object> result = Map.of(
                "deposits", enriched,
                "count", enriched.size()
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

            var enriched = page.getContent().stream().map(d -> {
                var m = new java.util.HashMap<String, Object>();
                m.put("id", d.getId());
                m.put("userId", d.getUserId());
                try {
                    java.util.Optional<User> u = userRepository.findById(d.getUserId());
                    m.put("username", u.map(User::getUsername).orElse(null));
                } catch (Exception ex) {
                    m.put("username", null);
                }
                m.put("amount", d.getAmount());
                m.put("fromAddress", d.getFromAddress());
                m.put("toAddress", d.getToAddress());
                m.put("txHash", d.getTxHash());
                m.put("createdAt", d.getCreatedAt());
                m.put("status", d.getStatus());
                return m;
            }).toList();

            Map<String, Object> result = Map.of(
                "deposits", enriched,
                "count", enriched.size()
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
     * GET /api/admin/deposits/history
     * Full paginated deposit history for admin UI (paginated)
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDepositsHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 500)));
            var pageRes = walletTransactionRepository.findByTransactionTypeOrderByCreatedAtDesc(
                WalletTransaction.TransactionType.DEPOSIT, pageable
            );

            var enriched = pageRes.getContent().stream().map(d -> {
                var m = new java.util.HashMap<String, Object>();
                m.put("id", d.getId());
                m.put("userId", d.getUserId());
                try {
                    java.util.Optional<User> u = userRepository.findById(d.getUserId());
                    m.put("username", u.map(User::getUsername).orElse(null));
                } catch (Exception ex) {
                    m.put("username", null);
                }
                m.put("amount", d.getAmount());
                m.put("fromAddress", d.getFromAddress());
                m.put("toAddress", d.getToAddress());
                m.put("txHash", d.getTxHash());
                m.put("createdAt", d.getCreatedAt());
                m.put("status", d.getStatus());
                return m;
            }).toList();

            Map<String, Object> result = Map.of(
                "deposits", enriched,
                "page", pageRes.getNumber(),
                "size", pageRes.getSize(),
                "totalElements", pageRes.getTotalElements(),
                "totalPages", pageRes.getTotalPages()
            );

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error fetching deposit history", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to fetch deposit history: " + e.getMessage())
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
