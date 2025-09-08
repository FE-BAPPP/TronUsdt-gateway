package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.WithdrawalTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WithdrawalTransactionRepository extends JpaRepository<WithdrawalTransaction, Long> {

    /**
     * Find withdrawal by ID and user ID
     */
    Optional<WithdrawalTransaction> findByIdAndUserId(Long id, UUID userId);

    /**
     * Find user withdrawals ordered by creation date
     */
    Page<WithdrawalTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find withdrawals by status
     */
    List<WithdrawalTransaction> findByStatus(WithdrawalTransaction.WithdrawalStatus status);

    /**
     * Find withdrawals by multiple statuses
     */
    List<WithdrawalTransaction> findByStatusIn(List<WithdrawalTransaction.WithdrawalStatus> statuses);

    /**
     * Count withdrawals by status
     */
    long countByStatus(WithdrawalTransaction.WithdrawalStatus status);

    /**
     * Sum withdrawal amounts by user and date range
     */
    @Query("SELECT SUM(w.amount) FROM WithdrawalTransaction w " +
           "WHERE w.userId = :userId " +
           "AND w.createdAt BETWEEN :startDate AND :endDate " +
           "AND w.status NOT IN ('CANCELLED', 'FAILED')")
    Optional<BigDecimal> sumAmountByUserIdAndDateRange(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find failed withdrawals that can be retried
     */
    @Query("SELECT w FROM WithdrawalTransaction w " +
           "WHERE w.status = 'FAILED' " +
           "AND w.retryCount < w.maxRetries " +
           "AND w.updatedAt < :beforeTime")
    List<WithdrawalTransaction> findFailedWithdrawalsForRetry(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * Find pending withdrawals older than specified time
     */
    @Query("SELECT w FROM WithdrawalTransaction w " +
           "WHERE w.status = 'PENDING' " +
           "AND w.createdAt < :beforeTime")
    List<WithdrawalTransaction> findStuckPendingWithdrawals(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * Find withdrawals by transaction hash
     */
    Optional<WithdrawalTransaction> findByTxHash(String txHash);

    /**
     * Get withdrawal statistics for admin dashboard
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN w.status = 'PENDING' THEN 1 END) as pending, " +
           "COUNT(CASE WHEN w.status = 'PROCESSING' THEN 1 END) as processing, " +
           "COUNT(CASE WHEN w.status = 'CONFIRMED' THEN 1 END) as confirmed, " +
           "COUNT(CASE WHEN w.status = 'FAILED' THEN 1 END) as failed, " +
           "SUM(CASE WHEN w.status = 'CONFIRMED' THEN w.amount ELSE 0 END) as totalConfirmed, " +
           "SUM(CASE WHEN w.status NOT IN ('CANCELLED', 'FAILED') THEN w.fee ELSE 0 END) as totalFees " +
           "FROM WithdrawalTransaction w " +
           "WHERE w.createdAt >= :startDate")
    Object[] getWithdrawalStats(@Param("startDate") LocalDateTime startDate);

    // --- Admin UI pageable helpers ---
    Page<WithdrawalTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<WithdrawalTransaction> findByStatusOrderByUpdatedAtDesc(
            WithdrawalTransaction.WithdrawalStatus status,
            Pageable pageable);

    // Auto-cancel helpers
    List<WithdrawalTransaction> findByStatusAndCreatedAtBefore(
            WithdrawalTransaction.WithdrawalStatus status,
            LocalDateTime createdAtBefore
    );
}
