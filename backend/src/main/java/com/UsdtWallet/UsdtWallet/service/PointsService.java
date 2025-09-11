package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.entity.PointsLedger;
import com.UsdtWallet.UsdtWallet.repository.PointsLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointsService {

    private final PointsLedgerRepository pointsLedgerRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationService notificationService;

    @Value("${points.exchange.rate:1.0}")
    private BigDecimal defaultExchangeRate; // 1 USDT = 1 Point

    @Value("${points.transfer.fee:0}")
    private BigDecimal transferFeeRate; // 0% fee by default

    private static final String BALANCE_CACHE_KEY = "user:balance:";
    private static final String TRANSFER_LOCK_KEY = "transfer:lock:";

    /**
     * Credit points for USDT deposit
     */
    @Transactional
    public boolean creditPointsForDeposit(UUID userId, BigDecimal pointsAmount,
                                        String transactionId, BigDecimal usdtAmount) {
        try {
            // Check if already credited
            if (pointsLedgerRepository.existsByTransactionId(transactionId)) {
                log.warn("Points already credited for transaction: {}", transactionId);
                return false;
            }

            BigDecimal currentBalance = getCurrentBalance(userId);
            BigDecimal newBalance = currentBalance.add(pointsAmount);

            PointsLedger ledgerEntry = PointsLedger.builder()
                .userId(userId)
                .transactionId(transactionId)
                .transactionType(PointsLedger.PointsTransactionType.DEPOSIT_CREDIT)
                .amount(pointsAmount)
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .usdtAmount(usdtAmount)
                .exchangeRate(defaultExchangeRate)
                .description("USDT deposit credit")
                .status(PointsLedger.PointsTransactionStatus.COMPLETED)
                .build();

            pointsLedgerRepository.save(ledgerEntry);

            // Update cached balance
            updateBalanceCache(userId, newBalance);

            log.info("✅ Credited {} points to user {} for USDT deposit", pointsAmount, userId);
            return true;

        } catch (Exception e) {
            log.error("Error crediting points for deposit: userId={}, amount={}", userId, pointsAmount, e);
            return false;
        }
    }

    /**
     * Transfer points between users (P2P)
     */
    @Transactional
    public boolean transferPoints(UUID fromUserId, UUID toUserId, BigDecimal amount, String description) {
        String lockKey = TRANSFER_LOCK_KEY + fromUserId;

        try {
            // Acquire lock to prevent double spending
            Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", 30, TimeUnit.SECONDS);

            if (!Boolean.TRUE.equals(lockAcquired)) {
                log.warn("Transfer already in progress for user: {}", fromUserId);
                return false;
            }

            // Validate users exist (add user validation if needed)

            // Check sender balance
            BigDecimal senderBalance = getCurrentBalance(fromUserId);
            if (senderBalance.compareTo(amount) < 0) {
                log.warn("Insufficient balance for transfer: user={}, balance={}, amount={}",
                    fromUserId, senderBalance, amount);
                return false;
            }

            // Calculate fee
            BigDecimal fee = amount.multiply(transferFeeRate);
            BigDecimal netAmount = amount.subtract(fee);

            if (netAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Transfer amount too small after fee: {}", netAmount);
                return false;
            }

            // Generate reference ID for this P2P transfer
            String referenceId = UUID.randomUUID().toString();

            // Debit from sender
            BigDecimal senderNewBalance = senderBalance.subtract(amount);
            PointsLedger senderEntry = PointsLedger.builder()
                .userId(fromUserId)
                .referenceId(referenceId)
                .transactionType(PointsLedger.PointsTransactionType.P2P_SEND)
                .amount(amount.negate()) // Negative for debit
                .balanceBefore(senderBalance)
                .balanceAfter(senderNewBalance)
                .toUserId(toUserId)
                .description(description != null ? description : "P2P transfer sent")
                .status(PointsLedger.PointsTransactionStatus.COMPLETED)
                .build();

            pointsLedgerRepository.save(senderEntry);

            // Credit to receiver
            BigDecimal receiverBalance = getCurrentBalance(toUserId);
            BigDecimal receiverNewBalance = receiverBalance.add(netAmount);

            PointsLedger receiverEntry = PointsLedger.builder()
                .userId(toUserId)
                .referenceId(referenceId)
                .transactionType(PointsLedger.PointsTransactionType.P2P_RECEIVE)
                .amount(netAmount)
                .balanceBefore(receiverBalance)
                .balanceAfter(receiverNewBalance)
                .fromUserId(fromUserId)
                .description(description != null ? description : "P2P transfer received")
                .status(PointsLedger.PointsTransactionStatus.COMPLETED)
                .build();

            pointsLedgerRepository.save(receiverEntry);

            // Handle fee if applicable
            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                // Fee entry for sender (additional debit)
                PointsLedger feeEntry = PointsLedger.builder()
                    .userId(fromUserId)
                    .referenceId(referenceId)
                    .transactionType(PointsLedger.PointsTransactionType.ADJUSTMENT)
                    .amount(fee.negate())
                    .balanceBefore(senderNewBalance)
                    .balanceAfter(senderNewBalance) // Already calculated above
                    .description("P2P transfer fee")
                    .status(PointsLedger.PointsTransactionStatus.COMPLETED)
                    .build();

                pointsLedgerRepository.save(feeEntry);
            }

            // Update cached balances
            updateBalanceCache(fromUserId, senderNewBalance);
            updateBalanceCache(toUserId, receiverNewBalance);

            log.info("✅ P2P transfer completed: {} points from {} to {} (net: {}, fee: {})",
                amount, fromUserId, toUserId, netAmount, fee);

            // Send notifications to both users
            notificationService.notifyPointsTransferred(fromUserId, amount, "user", false);
            notificationService.notifyPointsTransferred(toUserId, amount, "user", true);

            // Send balance updates
            BigDecimal fromBalance = getCurrentBalance(fromUserId);
            BigDecimal toBalance = getCurrentBalance(toUserId);
            notificationService.notifyBalanceUpdate(fromUserId, fromBalance);
            notificationService.notifyBalanceUpdate(toUserId, toBalance);

            return true;

        } catch (Exception e) {
            log.error("Error in P2P transfer: from={}, to={}, amount={}", fromUserId, toUserId, amount, e);
            return false;
        } finally {
            // Release lock
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * Get user's current points balance
     */
    public BigDecimal getCurrentBalance(UUID userId) {
        try {
            // Try cache first
            String cacheKey = BALANCE_CACHE_KEY + userId;
            Object cachedBalance = redisTemplate.opsForValue().get(cacheKey);

            if (cachedBalance instanceof Number) {
                return new BigDecimal(cachedBalance.toString());
            }

            // Calculate from database
            BigDecimal balance = pointsLedgerRepository.getCurrentBalance(userId);
            if (balance == null) {
                balance = BigDecimal.ZERO;
            }

            // Cache for future use
            redisTemplate.opsForValue().set(cacheKey, balance, 10, TimeUnit.MINUTES);

            return balance;

        } catch (Exception e) {
            log.error("Error getting balance for user: {}", userId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Deduct points for withdrawal
     */
    @Transactional
    public boolean deductPoints(UUID userId, BigDecimal amount, String description) {
        try {
            log.info("Deducting {} points from user: {}", amount, userId);

            BigDecimal currentBalance = getCurrentBalance(userId);
            if (currentBalance.compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance. Available: " + currentBalance + ", Required: " + amount);
            }

            BigDecimal newBalance = currentBalance.subtract(amount);

            // Create debit entry
            PointsLedger debitEntry = PointsLedger.builder()
                .userId(userId)
                .transactionType(PointsLedger.PointsTransactionType.WITHDRAWAL_DEBIT)
                .amount(amount.negate()) // Negative for debit
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .description(description)
                .transactionId("WITHDRAWAL_" + System.currentTimeMillis())
                .status(PointsLedger.PointsTransactionStatus.COMPLETED)
                .build();

            pointsLedgerRepository.save(debitEntry);

            // Update cache
            updateBalanceCache(userId, newBalance);

            log.info("Successfully deducted {} points from user: {}, new balance: {}",
                amount, userId, newBalance);
            return true;

        } catch (Exception e) {
            log.error("Error deducting points for user: {}", userId, e);
            throw new RuntimeException("Failed to deduct points: " + e.getMessage());
        }
    }

    /**
     * Add points (for refunds, bonuses, etc.)
     */
    @Transactional
    public boolean addPoints(UUID userId, BigDecimal amount, String description) {
        try {
            log.info("Adding {} points to user: {}", amount, userId);

            BigDecimal currentBalance = getCurrentBalance(userId);
            BigDecimal newBalance = currentBalance.add(amount);

            // Create credit entry
            PointsLedger creditEntry = PointsLedger.builder()
                .userId(userId)
                .transactionType(PointsLedger.PointsTransactionType.ADJUSTMENT)
                .amount(amount)
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .description(description)
                .transactionId("CREDIT_" + System.currentTimeMillis())
                .status(PointsLedger.PointsTransactionStatus.COMPLETED)
                .build();

            pointsLedgerRepository.save(creditEntry);

            // Update cache
            updateBalanceCache(userId, newBalance);

            log.info("Successfully added {} points to user: {}, new balance: {}",
                amount, userId, newBalance);
            return true;

        } catch (Exception e) {
            log.error("Error adding points for user: {}", userId, e);
            throw new RuntimeException("Failed to add points: " + e.getMessage());
        }
    }

    /**
     * Update balance cache
     */
    private void updateBalanceCache(UUID userId, BigDecimal newBalance) {
        try {
            redisTemplate.opsForValue().set(
                BALANCE_CACHE_KEY + userId,
                newBalance.toString(),
                30,
                TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("Failed to update balance cache for user: {}", userId, e);
        }
    }

    /**
     * Admin adjustment (bonus, refund, etc.)
     */
    @Transactional
    public boolean adjustBalance(UUID userId, BigDecimal amount, String reason,
                               PointsLedger.PointsTransactionType type) {
        try {
            BigDecimal currentBalance = getCurrentBalance(userId);
            BigDecimal newBalance = currentBalance.add(amount);

            // Prevent negative balance for debits
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Adjustment would result in negative balance: user={}, current={}, adjustment={}",
                    userId, currentBalance, amount);
                return false;
            }

            PointsLedger adjustment = PointsLedger.builder()
                .userId(userId)
                .transactionType(type)
                .amount(amount)
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .description(reason)
                .status(PointsLedger.PointsTransactionStatus.COMPLETED)
                .build();

            pointsLedgerRepository.save(adjustment);
            updateBalanceCache(userId, newBalance);

            log.info("✅ Balance adjusted: user={}, amount={}, reason={}", userId, amount, reason);
            return true;

        } catch (Exception e) {
            log.error("Error adjusting balance: userId={}, amount={}", userId, amount, e);
            return false;
        }
    }

    /**
     * Get user's points transaction history
     */
    public List<PointsLedger> getTransactionHistory(UUID userId, int limit) {
        return pointsLedgerRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .limit(limit)
            .toList();
    }

    /**
     * Get user's P2P transaction history
     */
    public List<PointsLedger> getP2PHistory(UUID userId) {
        return pointsLedgerRepository.findP2PTransactionsByUserId(userId);
    }

    /**
     * Get total statistics for user
     */
    public Map<String, Object> getUserStats(UUID userId) {
        BigDecimal currentBalance = getCurrentBalance(userId);
        BigDecimal totalDeposits = pointsLedgerRepository.getTotalDepositCredits(userId);
        BigDecimal totalSent = pointsLedgerRepository.getTotalP2PSent(userId);
        BigDecimal totalReceived = pointsLedgerRepository.getTotalP2PReceived(userId);

        return Map.of(
            "currentBalance", currentBalance,
            "totalDeposits", totalDeposits,
            "totalSent", totalSent.abs(), // Make positive for display
            "totalReceived", totalReceived,
            "netP2P", totalReceived.subtract(totalSent.abs())
        );
    }

    /**
     * Validate if user has sufficient balance
     */
    public boolean hasSufficientBalance(UUID userId, BigDecimal amount) {
        BigDecimal currentBalance = getCurrentBalance(userId);
        return currentBalance.compareTo(amount) >= 0;
    }

    /**
     * Available balance = latest COMPLETED balance - pending withdrawal locks
     */
    public BigDecimal getAvailableBalance(UUID userId) {
        BigDecimal current = getCurrentBalance(userId);
        BigDecimal pendingLocks = pointsLedgerRepository.getTotalPendingWithdrawalLocks(userId);
        // pendingLocks is negative values summed, e.g. -100 => subtracting a negative equals adding
        return current.add(pendingLocks); // because locks are negative amounts
    }

    /**
     * Create a PENDING lock ledger for withdrawal, idempotent by transactionId
     */
    @Transactional
    public boolean lockPointsForWithdrawal(UUID userId, BigDecimal amount, String withdrawalId) {
        String lockTxId = "WITHDRAWAL_LOCK_" + withdrawalId;
        if (pointsLedgerRepository.existsByTransactionId(lockTxId)) {
            log.info("Lock already exists: {}", lockTxId);
            return true;
        }
        BigDecimal available = getAvailableBalance(userId);
        if (available.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient available balance");
        }
        // Create PENDING lock entry (negative amount)
        PointsLedger lockEntry = PointsLedger.builder()
            .userId(userId)
            .transactionId(lockTxId)
            .transactionType(PointsLedger.PointsTransactionType.WITHDRAWAL_DEBIT)
            .amount(amount.negate())
            .balanceBefore(getCurrentBalance(userId))
            .balanceAfter(getCurrentBalance(userId)) // unchanged for PENDING
            .description("Lock for withdrawal " + withdrawalId)
            .status(PointsLedger.PointsTransactionStatus.PENDING)
            .build();
        pointsLedgerRepository.save(lockEntry);
        log.info("Locked {} points for withdrawal {} (txId={})", amount, withdrawalId, lockTxId);
        return true;
    }

    /**
     * Cancel PENDING lock for withdrawal
     */
    @Transactional
    public void unlockPointsForWithdrawal(UUID userId, String withdrawalId) {
        String lockTxId = "WITHDRAWAL_LOCK_" + withdrawalId;
        Optional<PointsLedger> lockOpt = pointsLedgerRepository.findFirstByTransactionId(lockTxId);
        if (lockOpt.isEmpty()) return;
        PointsLedger lock = lockOpt.get();
        if (lock.getStatus() == PointsLedger.PointsTransactionStatus.PENDING) {
            lock.setStatus(PointsLedger.PointsTransactionStatus.CANCELLED);
            pointsLedgerRepository.save(lock);
            log.info("Unlocked points lock {} for user {}", lockTxId, userId);
        }
    }

    /**
     * Finalize withdrawal: record a COMPLETED debit and close the lock (if exists)
     */
    @Transactional
    public void finalizeWithdrawalDebit(UUID userId, BigDecimal amount, String withdrawalId) {
        String debitTxId = "WITHDRAWAL_DEBIT_" + withdrawalId;
        if (!pointsLedgerRepository.existsByTransactionId(debitTxId)) {
            BigDecimal currentBalance = getCurrentBalance(userId);
            BigDecimal newBalance = currentBalance.subtract(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Insufficient balance to finalize withdrawal");
            }
            PointsLedger debit = PointsLedger.builder()
                .userId(userId)
                .transactionId(debitTxId)
                .transactionType(PointsLedger.PointsTransactionType.WITHDRAWAL_DEBIT)
                .amount(amount.negate())
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .description("Finalize withdrawal " + withdrawalId)
                .status(PointsLedger.PointsTransactionStatus.COMPLETED)
                .build();
            pointsLedgerRepository.save(debit);
            updateBalanceCache(userId, newBalance);
            log.info("Finalized withdrawal debit {} for user {} amount {}", withdrawalId, userId, amount);
        }
        // Close lock if present
        unlockPointsForWithdrawal(userId, withdrawalId);
    }
}
