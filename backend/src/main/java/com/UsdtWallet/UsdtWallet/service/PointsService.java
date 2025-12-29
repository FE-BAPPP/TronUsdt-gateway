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
     * 💰 LUỒNG DEPOSIT: Cộng Points khi user nạp USDT
     * 
     * Employer nạp USDT → hệ thống verify → cộng Points (1 USDT = 1 Point)
     * 
     * @param userId ID của user nhận points
     * @param pointsAmount Số lượng points cộng vào (= USDT amount)
     * @param transactionId Transaction ID blockchain (để tránh duplicate)
     * @param usdtAmount Số USDT thực tế đã nạp
     * @return true nếu thành công
     */
    @Transactional
    public boolean creditPointsForDeposit(UUID userId, BigDecimal pointsAmount,
                                        String transactionId, BigDecimal usdtAmount) {
        try {
            // ✅ Kiểm tra idempotency - tránh cộng duplicate
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

    // ========================================================================
    // LUỒNG NGHIỆP VỤ CHÍNH: DEPOSIT → ESCROW → MILESTONE PAYMENT → WITHDRAWAL
    // ========================================================================
    // P2P Transfer đã được XÓA - không thuộc luồng nghiệp vụ freelance platform

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
     * 📊 Thống kê tổng quan cho user
     * - Tổng nạp (deposits)
     * - Số dư hiện tại
     * - Tổng thu nhập từ projects (escrow releases)
     */
    public Map<String, Object> getUserStats(UUID userId) {
        BigDecimal currentBalance = getCurrentBalance(userId);
        BigDecimal totalDeposits = pointsLedgerRepository.getTotalDepositCredits(userId);

        return Map.of(
            "currentBalance", currentBalance,
            "totalDeposits", totalDeposits
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
     * Available balance = current balance - pending withdrawal locks - pending escrow locks
     */
    public BigDecimal getAvailableBalance(UUID userId) {
        BigDecimal current = getCurrentBalance(userId);
        BigDecimal pendingWithdrawals = pointsLedgerRepository.getTotalPendingWithdrawalLocks(userId);
        BigDecimal pendingEscrows = pointsLedgerRepository.getTotalPendingEscrowLocks(userId);
        
        // Both locks are negative, so adding them reduces available balance
        return current.add(pendingWithdrawals).add(pendingEscrows);
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

    /**
     * 🔒 LUỒNG ESCROW: Lock points khi Employer hire Freelancer
     * 
     * Khi Employer chấp nhận proposal:
     * 1. Lock số tiền project vào escrow (PENDING status)
     * 2. Points vẫn trong ví nhưng không thể rút
     * 3. Chỉ được release khi milestone approved
     * 
     * @param userId ID của Employer
     * @param amount Số tiền cần lock (= agreed amount)
     * @param projectId ID của project
     * @return true nếu lock thành công
     */
    @Transactional
    public boolean lockPointsForProject(UUID userId, BigDecimal amount, String projectId) {
        String lockTxId = "PROJECT_ESCROW_" + projectId;
        
        // ✅ Idempotency check
        if (pointsLedgerRepository.existsByTransactionId(lockTxId)) {
            log.info("Escrow lock already exists: {}", lockTxId);
            return true;
        }
        
        // ✅ Kiểm tra số dư available (trừ các lock khác)
        BigDecimal available = getAvailableBalance(userId);
        if (available.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient available balance for escrow lock. Available: " + available + ", Required: " + amount);
        }
        
        // Create PENDING lock entry
        PointsLedger lockEntry = PointsLedger.builder()
            .userId(userId)
            .transactionId(lockTxId)
            .transactionType(PointsLedger.PointsTransactionType.ESCROW_LOCK)
            .amount(amount.negate()) // Negative for lock
            .balanceBefore(getCurrentBalance(userId))
            .balanceAfter(getCurrentBalance(userId)) // Balance unchanged (just locked)
            .description("Escrow lock for project " + projectId)
            .status(PointsLedger.PointsTransactionStatus.PENDING)
            .build();
        
        pointsLedgerRepository.save(lockEntry);
        log.info("✅ Locked {} points for project {} (txId={})", amount, projectId, lockTxId);
        
        // Invalidate cache
        updateBalanceCache(userId, getCurrentBalance(userId));
        
        return true;
    }

    /**
     * ✅ LUỒNG MILESTONE PAYMENT: Giải ngân từ Escrow cho Freelancer
     * 
     * Khi Employer approve milestone:
     * 1. Đóng lock escrow (PENDING → COMPLETED)
     * 2. Trừ tiền từ ví Employer
     * 3. Cộng tiền vào ví Freelancer
     * 4. Ghi nhận transaction cho cả 2 bên
     * 
     * @param employerId ID của Employer (người trả tiền)
     * @param freelancerId ID của Freelancer (người nhận tiền)
     * @param amount Số tiền milestone
     * @param projectId ID của project
     * @return true nếu release thành công
     */
    @Transactional
    public boolean releaseEscrowToFreelancer(UUID employerId, UUID freelancerId, 
                                        BigDecimal amount, String projectId) {
        String lockTxId = "PROJECT_ESCROW_" + projectId;
        String releaseTxId = "ESCROW_RELEASE_" + projectId + "_" + System.currentTimeMillis();
        
        // BƯỚC 1: Đóng escrow lock (PENDING → COMPLETED)
        Optional<PointsLedger> lockOpt = pointsLedgerRepository.findFirstByTransactionId(lockTxId);
        if (lockOpt.isPresent()) {
            PointsLedger lock = lockOpt.get();
            if (lock.getStatus() == PointsLedger.PointsTransactionStatus.PENDING) {
                lock.setStatus(PointsLedger.PointsTransactionStatus.COMPLETED);
                pointsLedgerRepository.save(lock);
                log.info("🔓 Escrow lock closed: {}", lockTxId);
            }
        }
        
        // BƯỚC 2: Trừ tiền từ ví Employer (DEBIT)
        BigDecimal employerBalance = getCurrentBalance(employerId);
        BigDecimal employerNewBalance = employerBalance.subtract(amount);
        
        PointsLedger employerDebit = PointsLedger.builder()
            .userId(employerId)
            .transactionId(releaseTxId + "_DEBIT")
            .transactionType(PointsLedger.PointsTransactionType.ESCROW_RELEASE)
            .amount(amount.negate()) // Số âm = trừ tiền
            .balanceBefore(employerBalance)
            .balanceAfter(employerNewBalance)
            .toUserId(freelancerId)
            .description("💸 Milestone payment to freelancer - Project " + projectId)
            .status(PointsLedger.PointsTransactionStatus.COMPLETED)
            .build();
        
        pointsLedgerRepository.save(employerDebit);
        updateBalanceCache(employerId, employerNewBalance);
        log.info("💸 Deducted {} from Employer {}", amount, employerId);
        
        // BƯỚC 3: Cộng tiền vào ví Freelancer (CREDIT)
        BigDecimal freelancerBalance = getCurrentBalance(freelancerId);
        BigDecimal freelancerNewBalance = freelancerBalance.add(amount);
        
        PointsLedger freelancerCredit = PointsLedger.builder()
            .userId(freelancerId)
            .transactionId(releaseTxId + "_CREDIT")
            .transactionType(PointsLedger.PointsTransactionType.ESCROW_RELEASE)
            .amount(amount) // Số dương = nhận tiền
            .balanceBefore(freelancerBalance)
            .balanceAfter(freelancerNewBalance)
            .fromUserId(employerId)
            .description("💰 Milestone payment received - Project " + projectId)
            .status(PointsLedger.PointsTransactionStatus.COMPLETED)
            .build();
        
        pointsLedgerRepository.save(freelancerCredit);
        updateBalanceCache(freelancerId, freelancerNewBalance);
        log.info("💰 Credited {} to Freelancer {}", amount, freelancerId);
        
        log.info("✅ MILESTONE PAYMENT COMPLETED: {} Points | Employer {} → Freelancer {} | Project {}", 
            amount, employerId, freelancerId, projectId);
        
        return true;
    }

    /**
     * Refund escrow to employer
     */
    @Transactional
    public boolean refundEscrow(UUID employerId, UUID freelancerId, 
                               BigDecimal amount, String projectId) {
        String lockTxId = "PROJECT_ESCROW_" + projectId;
        String refundTxId = "ESCROW_REFUND_" + projectId + "_" + System.currentTimeMillis();
        
        // 1. Close the lock
        Optional<PointsLedger> lockOpt = pointsLedgerRepository.findFirstByTransactionId(lockTxId);
        if (lockOpt.isPresent()) {
            PointsLedger lock = lockOpt.get();
            if (lock.getStatus() == PointsLedger.PointsTransactionStatus.PENDING) {
                lock.setStatus(PointsLedger.PointsTransactionStatus.CANCELLED);
                pointsLedgerRepository.save(lock);
            }
        }
        
        // 2. Refund to employer
        BigDecimal employerBalance = getCurrentBalance(employerId);
        BigDecimal employerNewBalance = employerBalance.add(amount);
        
        PointsLedger refund = PointsLedger.builder()
            .userId(employerId)
            .transactionId(refundTxId)
            .transactionType(PointsLedger.PointsTransactionType.ESCROW_REFUND)
            .amount(amount)
            .balanceBefore(employerBalance)
            .balanceAfter(employerNewBalance)
            .description("Refund from project " + projectId)
            .status(PointsLedger.PointsTransactionStatus.COMPLETED)
            .build();
        
        pointsLedgerRepository.save(refund);
        updateBalanceCache(employerId, employerNewBalance);
        
        log.info("✅ Escrow refunded: {} USDT to employer {} for project {}", 
            amount, employerId, projectId);
        
        return true;
    }

    /**
     * Lock funds to escrow for milestone
     * 
     * @param employerId ID của employer
     * @param projectId ID của project
     * @param milestoneId ID của milestone
     * @param amount Số tiền cần lock
     * @return true if successful
     */
    @Transactional
    public boolean lockFundsToEscrow(UUID employerId, UUID projectId, UUID milestoneId, BigDecimal amount) {
        // Check balance
        BigDecimal currentBalance = getCurrentBalance(employerId);
        if (currentBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance to lock funds");
        }

        // Create lock transaction
        BigDecimal newBalance = currentBalance.subtract(amount);
        String transactionId = "ESCROW_LOCK_" + milestoneId;

        PointsLedger lock = PointsLedger.builder()
                .userId(employerId)
                .transactionId(transactionId)
                .transactionType(PointsLedger.PointsTransactionType.ESCROW_LOCK)
                .amount(amount.negate()) // Negative amount = deduction
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .description("Lock funds for milestone " + milestoneId)
                .referenceId(milestoneId.toString())
                .status(PointsLedger.PointsTransactionStatus.COMPLETED)
                .build();

        pointsLedgerRepository.save(lock);
        updateBalanceCache(employerId, newBalance);

        log.info("✅ Locked {} PTS from employer {} for milestone {}", amount, employerId, milestoneId);
        return true;
    }

    /**
     * Release escrow to freelancer for milestone
     * 
     * @param freelancerId ID của freelancer
     * @param projectId ID của project
     * @param milestoneId ID của milestone
     * @param amount Số tiền release
     * @return true if successful
     */
    @Transactional
    public boolean releaseEscrowToFreelancer(UUID freelancerId, UUID projectId, UUID milestoneId, BigDecimal amount) {
        BigDecimal currentBalance = getCurrentBalance(freelancerId);
        BigDecimal newBalance = currentBalance.add(amount);
        String transactionId = "ESCROW_RELEASE_" + milestoneId;

        PointsLedger release = PointsLedger.builder()
                .userId(freelancerId)
                .transactionId(transactionId)
                .transactionType(PointsLedger.PointsTransactionType.ESCROW_RELEASE)
                .amount(amount)
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .description("Payment for milestone " + milestoneId)
                .referenceId(milestoneId.toString())
                .status(PointsLedger.PointsTransactionStatus.COMPLETED)
                .build();

        pointsLedgerRepository.save(release);
        updateBalanceCache(freelancerId, newBalance);

        log.info("✅ Released {} PTS to freelancer {} for milestone {}", amount, freelancerId, milestoneId);
        return true;
    }

    /**
     * Refund escrow to employer
     * 
     * @param employerId ID của employer
     * @param projectId ID của project
     * @param milestoneId ID của milestone
     * @param amount Số tiền refund
     * @return true if successful
     */
    @Transactional
    public boolean refundEscrowToEmployer(UUID employerId, UUID projectId, UUID milestoneId, BigDecimal amount) {
        BigDecimal currentBalance = getCurrentBalance(employerId);
        BigDecimal newBalance = currentBalance.add(amount);
        String transactionId = "ESCROW_REFUND_" + milestoneId;

        PointsLedger refund = PointsLedger.builder()
                .userId(employerId)
                .transactionId(transactionId)
                .transactionType(PointsLedger.PointsTransactionType.ESCROW_REFUND)
                .amount(amount)
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .description("Refund for milestone " + milestoneId)
                .referenceId(milestoneId.toString())
                .status(PointsLedger.PointsTransactionStatus.COMPLETED)
                .build();

        pointsLedgerRepository.save(refund);
        updateBalanceCache(employerId, newBalance);

        log.info("✅ Refunded {} PTS to employer {} for milestone {}", amount, employerId, milestoneId);
        return true;
    }
}
