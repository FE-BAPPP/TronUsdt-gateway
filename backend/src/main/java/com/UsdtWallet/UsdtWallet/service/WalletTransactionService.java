package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.entity.WalletTransaction;
import com.UsdtWallet.UsdtWallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;

    public Page<WalletTransaction> getDepositHistoryByUserId(String userId, Pageable pageable) {
        return walletTransactionRepository.findByUserIdAndTransactionTypeAndDirection(
            userId,
            WalletTransaction.TransactionType.DEPOSIT,
            WalletTransaction.TransactionDirection.IN,
            pageable
        );
    }

    public List<WalletTransaction> getPendingDepositsByUserId(String userId) {
        return walletTransactionRepository.findByUserIdAndTransactionTypeAndStatus(
            userId,
            WalletTransaction.TransactionType.DEPOSIT,
            WalletTransaction.TransactionStatus.PENDING
        );
    }

    public WalletTransaction getDepositByIdAndUserId(Long id, String userId) {
        return walletTransactionRepository.findByIdAndUserIdAndTransactionType(
            id, userId, WalletTransaction.TransactionType.DEPOSIT
        ).orElse(null);
    }

    public BigDecimal getTotalDepositedByUserId(String userId) {
        return walletTransactionRepository.sumAmountByUserIdAndTransactionTypeAndStatus(
            userId,
            WalletTransaction.TransactionType.DEPOSIT,
            WalletTransaction.TransactionStatus.CONFIRMED
        );
    }

    public Long getDepositCountByUserId(String userId) {
        return walletTransactionRepository.countByUserIdAndTransactionType(
            userId, WalletTransaction.TransactionType.DEPOSIT
        );
    }

    public WalletTransaction getLastDepositByUserId(String userId) {
        return walletTransactionRepository.findFirstByUserIdAndTransactionTypeOrderByCreatedAtDesc(
            userId, WalletTransaction.TransactionType.DEPOSIT
        ).orElse(null);
    }

    public Long getPendingDepositCountByUserId(String userId) {
        return walletTransactionRepository.countByUserIdAndTransactionTypeAndStatus(
            userId,
            WalletTransaction.TransactionType.DEPOSIT,
            WalletTransaction.TransactionStatus.PENDING
        );
    }

    public WalletTransaction getTransactionByTxHashAndUserId(String txHash, String userId) {
        return walletTransactionRepository.findByTxHashAndUserId(txHash, userId).orElse(null);
    }

    // Thêm các methods mới cho TransactionController
    public Page<WalletTransaction> getFilteredTransactions(String userId, String type, String status,
                                                         String startDate, String endDate, Pageable pageable) {
        // Implement filtering logic
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public WalletTransaction getTransactionByIdAndUserId(Long id, String userId) {
        return walletTransactionRepository.findByIdAndUserIdAndTransactionType(
            id, userId, null // Lấy tất cả types
        ).orElse(null);
    }

    public Map<String, Object> getTransactionSummary(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> summaryData = walletTransactionRepository.getTransactionSummaryByUserId(userId);

        Map<String, Object> summary = new HashMap<>();
        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        Long totalTransactions = 0L;

        for (Object[] row : summaryData) {
            WalletTransaction.TransactionType type = (WalletTransaction.TransactionType) row[0];
            Long count = (Long) row[1];
            BigDecimal amount = (BigDecimal) row[2];

            totalTransactions += count;

            if (type == WalletTransaction.TransactionType.DEPOSIT) {
                totalDeposits = amount;
            } else if (type == WalletTransaction.TransactionType.WITHDRAWAL) {
                totalWithdrawals = amount;
            }
        }

        summary.put("totalDeposits", totalDeposits);
        summary.put("totalWithdrawals", totalWithdrawals);
        summary.put("totalTransactions", totalTransactions);
        summary.put("netAmount", totalDeposits.subtract(totalWithdrawals));

        return summary;
    }

    public List<WalletTransaction> getTransactionsByDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        return walletTransactionRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    public List<WalletTransaction> getTransactionsForExport(String userId, String startDate, String endDate) {
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
            return walletTransactionRepository.findByUserIdAndDateRange(userId, start, end);
        }
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<WalletTransaction> searchTransactions(String userId, String keyword, int limit) {
        // Implement search logic - search by txHash, amount, etc.
        List<WalletTransaction> allTransactions = walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return allTransactions.stream()
            .filter(tx -> tx.getTxHash().toLowerCase().contains(keyword.toLowerCase()) ||
                         tx.getAmount().toString().contains(keyword))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
}

