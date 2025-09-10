package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.entity.WalletTransaction;
import com.UsdtWallet.UsdtWallet.service.WalletTransactionService;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class DepositUserController {

    private final WalletTransactionService walletTransactionService;

    /**
     * Lấy lịch sử nạp USDT (đã quy đổi sang points)
     */
    @GetMapping("/history")
    public ResponseEntity<Page<WalletTransaction>> getDepositHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Lấy lịch sử deposits đã được credit points
        Page<WalletTransaction> deposits = walletTransactionService
            .getDepositHistoryByUserId(currentUser.getId(), pageable);

        return ResponseEntity.ok(deposits);
    }

    /**
     * Lấy các giao dịch nạp đang pending (chưa confirm hoặc chưa credit points)
     */
    @GetMapping("/pending")
    public ResponseEntity<List<WalletTransaction>> getPendingDeposits(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<WalletTransaction> pendingDeposits = walletTransactionService
            .getPendingDepositsByUserId(currentUser.getId());

        return ResponseEntity.ok(pendingDeposits);
    }

    /**
     * Kiểm tra trạng thái giao dịch nạp theo txHash
     */
    @GetMapping("/status/{txHash}")
    public ResponseEntity<Map<String, Object>> checkDepositStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String txHash) {

        WalletTransaction transaction = walletTransactionService
            .getTransactionByTxHashAndUserId(txHash, currentUser.getId());

        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> status = new HashMap<>();
        status.put("txHash", transaction.getTxHash());
        status.put("status", transaction.getStatus());
        status.put("usdtAmount", transaction.getAmount()); // Số USDT đã nạp
        status.put("pointsCredited", transaction.getPointsCredited()); // Điểm đã được credit
        status.put("confirmationCount", transaction.getConfirmationCount());
        status.put("blockNumber", transaction.getBlockNumber());
        status.put("createdAt", transaction.getCreatedAt());
        status.put("isSwept", transaction.getIsSwept()); // Đã sweep về master wallet chưa
        status.put("sweptAt", transaction.getSweptAt());

        return ResponseEntity.ok(status);
    }
}
