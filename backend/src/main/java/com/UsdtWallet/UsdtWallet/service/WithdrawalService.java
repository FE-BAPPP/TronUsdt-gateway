package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.dto.request.WithdrawalRequest;
import com.UsdtWallet.UsdtWallet.model.entity.WithdrawalTransaction;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import com.UsdtWallet.UsdtWallet.repository.WithdrawalTransactionRepository;
import com.UsdtWallet.UsdtWallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    private final WithdrawalTransactionRepository withdrawalRepository;
    private final UserRepository userRepository;
    private final PointsService pointsService;
    private final PasswordEncoder passwordEncoder;
    private final WithdrawalQueueService withdrawalQueueService;

    @Value("${withdrawal.fee.fixed:1.0}")
    private BigDecimal fixedFee;

    @Value("${withdrawal.fee.percentage:0.001}")
    private BigDecimal feePercentage;

    @Value("${withdrawal.min.amount:1.0}")
    private BigDecimal minWithdrawalAmount;

    @Value("${withdrawal.max.amount:100000.0}")
    private BigDecimal maxWithdrawalAmount;

    @Value("${withdrawal.daily.limit:50000.0}")
    private BigDecimal dailyLimit;

    /**
     * Create withdrawal request
     */
    @Transactional
    public WithdrawalTransaction createWithdrawal(UUID userId, WithdrawalRequest request) {
        log.info("Creating withdrawal for user: {}, amount: {}", userId, request.getAmount());

        // Validate user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // withdrawal lock after password reset
        if (user.getWithdrawalsDisabledUntil() != null && LocalDateTime.now().isBefore(user.getWithdrawalsDisabledUntil())) {
            throw new RuntimeException("Withdrawals are temporarily disabled until " + user.getWithdrawalsDisabledUntil() + " due to a recent security change.");
        }

        // Validate password if provided
        if (request.getPassword() != null && !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Validate withdrawal limits
        validateWithdrawalLimits(userId, request.getAmount());

        // Calculate fee and net amount
        BigDecimal fee = calculateFee(request.getAmount());
        BigDecimal netAmount = request.getAmount().subtract(fee);

        // Check user balance
        BigDecimal userBalance = pointsService.getCurrentBalance(userId.toString());
        if (userBalance.compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance. Available: " + userBalance + " USDT");
        }

        // Create withdrawal transaction
        WithdrawalTransaction withdrawal = WithdrawalTransaction.builder()
            .userId(userId)
            .toAddress(request.getToAddress())
            .amount(request.getAmount())
            .fee(fee)
            .netAmount(netAmount)
            .status(WithdrawalTransaction.WithdrawalStatus.PENDING)
            .build();

        withdrawal = withdrawalRepository.save(withdrawal);

        // Deduct amount from user balance
        pointsService.deductPoints(userId.toString(), request.getAmount(),
            "Withdrawal to " + request.getToAddress());

        // Add to processing queue
        withdrawalQueueService.addToQueue(withdrawal.getId());

        log.info("Withdrawal created successfully: ID={}, Amount={}, Fee={}",
            withdrawal.getId(), withdrawal.getAmount(), withdrawal.getFee());

        return withdrawal;
    }

    /**
     * Get user withdrawal history
     */
    public Map<String, Object> getUserWithdrawalHistory(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<WithdrawalTransaction> withdrawals = withdrawalRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);

        List<Map<String, Object>> withdrawalList = withdrawals.getContent().stream()
            .map(this::convertToDto)
            .toList();

        return Map.of(
            "withdrawals", withdrawalList,
            "totalElements", withdrawals.getTotalElements(),
            "totalPages", withdrawals.getTotalPages(),
            "currentPage", page,
            "pageSize", size
        );
    }

    /**
     * Get withdrawal status
     */
    public Map<String, Object> getWithdrawalStatus(Long withdrawalId, UUID userId) {
        WithdrawalTransaction withdrawal = withdrawalRepository.findByIdAndUserId(withdrawalId, userId)
            .orElseThrow(() -> new RuntimeException("Withdrawal not found"));

        return convertToDto(withdrawal);
    }

    /**
     * Cancel withdrawal
     */
    @Transactional
    public boolean cancelWithdrawal(Long withdrawalId, UUID userId) {
        WithdrawalTransaction withdrawal = withdrawalRepository.findByIdAndUserId(withdrawalId, userId)
            .orElseThrow(() -> new RuntimeException("Withdrawal not found"));

        if (!withdrawal.canBeCancelled()) {
            throw new RuntimeException("Cannot cancel withdrawal in current status: " + withdrawal.getStatus());
        }

        withdrawal.setStatus(WithdrawalTransaction.WithdrawalStatus.CANCELLED);
        withdrawalRepository.save(withdrawal);

        // Refund amount to user balance
        pointsService.addPoints(userId.toString(), withdrawal.getAmount(),
            "Withdrawal cancellation refund");

        log.info("Withdrawal cancelled: ID={}, Amount={}", withdrawalId, withdrawal.getAmount());
        return true;
    }

    /**
     * Get withdrawal limits and fees
     */
    public Map<String, Object> getWithdrawalLimits(UUID userId) {
        BigDecimal dailyUsed = getDailyWithdrawalAmount(userId);
        BigDecimal remainingDaily = dailyLimit.subtract(dailyUsed);

        return Map.of(
            "minAmount", minWithdrawalAmount,
            "maxAmount", maxWithdrawalAmount,
            "dailyLimit", dailyLimit,
            "dailyUsed", dailyUsed,
            "remainingDaily", remainingDaily.max(BigDecimal.ZERO),
            "fixedFee", fixedFee,
            "feePercentage", feePercentage.multiply(new BigDecimal("100")) + "%"
        );
    }

    /**
     * Calculate withdrawal fee
     */
    private BigDecimal calculateFee(BigDecimal amount) {
        BigDecimal percentageFee = amount.multiply(feePercentage);
        return fixedFee.add(percentageFee).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Validate withdrawal limits
     */
    private void validateWithdrawalLimits(UUID userId, BigDecimal amount) {
        // Check minimum amount
        if (amount.compareTo(minWithdrawalAmount) < 0) {
            throw new RuntimeException("Minimum withdrawal amount is " + minWithdrawalAmount + " USDT");
        }

        // Check maximum amount
        if (amount.compareTo(maxWithdrawalAmount) > 0) {
            throw new RuntimeException("Maximum withdrawal amount is " + maxWithdrawalAmount + " USDT");
        }

        // Check daily limit
        BigDecimal dailyUsed = getDailyWithdrawalAmount(userId);
        if (dailyUsed.add(amount).compareTo(dailyLimit) > 0) {
            BigDecimal remaining = dailyLimit.subtract(dailyUsed);
            throw new RuntimeException("Daily withdrawal limit exceeded. Remaining: " + remaining + " USDT");
        }
    }

    /**
     * Get daily withdrawal amount
     */
    private BigDecimal getDailyWithdrawalAmount(UUID userId) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return withdrawalRepository.sumAmountByUserIdAndDateRange(userId, startOfDay, endOfDay)
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Convert withdrawal to DTO
     */
    private Map<String, Object> convertToDto(WithdrawalTransaction withdrawal) {
        // Create HashMap instead of Map.of() to avoid 10+ parameter limit
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", withdrawal.getId());
        dto.put("toAddress", withdrawal.getToAddress());
        dto.put("amount", withdrawal.getAmount());
        dto.put("fee", withdrawal.getFee());
        dto.put("netAmount", withdrawal.getNetAmount());
        dto.put("status", withdrawal.getStatus());
        dto.put("txHash", withdrawal.getTxHash() != null ? withdrawal.getTxHash() : "");
        dto.put("confirmations", withdrawal.getConfirmations());
        dto.put("createdAt", withdrawal.getCreatedAt());
        dto.put("processedAt", withdrawal.getProcessedAt());
        dto.put("confirmedAt", withdrawal.getConfirmedAt());
        dto.put("failureReason", withdrawal.getFailureReason() != null ? withdrawal.getFailureReason() : "");

        return dto;
    }
}
