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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class TransactionController {

    private final WalletTransactionService walletTransactionService;

    /**
     * Lấy tất cả giao dịch của user với phân trang và filter
     */
    @GetMapping
    public ResponseEntity<Page<WalletTransaction>> getAllTransactions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WalletTransaction> transactions = walletTransactionService
            .getFilteredTransactions(currentUser.getId().toString(), type, status,
                                   startDate, endDate, pageable);

        return ResponseEntity.ok(transactions);
    }

    /**
     * Lấy chi tiết giao dịch cụ thể theo ID
     */
    @GetMapping("/{txId}")
    public ResponseEntity<WalletTransaction> getTransactionDetails(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long txId) {

        WalletTransaction transaction = walletTransactionService
            .getTransactionByIdAndUserId(txId, currentUser.getId().toString());

        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(transaction);
    }

    /**
     * Lấy tổng hợp giao dịch theo thời gian
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getTransactionSummary(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "30") int days) {

        String userId = currentUser.getId().toString();
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        Map<String, Object> summary = walletTransactionService
            .getTransactionSummary(userId, startDate, endDate);

        return ResponseEntity.ok(summary);
    }
}
