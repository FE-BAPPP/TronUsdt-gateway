package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.WalletTransaction;
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

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, String> {

    // --- Các method cho DepositScanner / Sweep ---

    // Find by transaction hash
    Optional<WalletTransaction> findByTxHash(String txHash);

    // Find deposit by sweep tx hash
    Optional<WalletTransaction> findBySweepTxHash(String sweepTxHash);

    // Find transactions by user
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(String userId);

    // Find transactions by address
    List<WalletTransaction> findByToAddressOrderByCreatedAtDesc(String toAddress);

    // Find unswept deposits
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.transactionType = 'DEPOSIT' " +
            "AND wt.status = 'CONFIRMED' AND wt.isSwept = false")
    List<WalletTransaction> findUnsweptDeposits();

    // Find deposits by address that are not swept
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.toAddress = :address " +
            "AND wt.transactionType = 'DEPOSIT' AND wt.status = 'CONFIRMED' " +
            "AND wt.isSwept = false")
    List<WalletTransaction> findUnsweptDepositsByAddress(@Param("address") String address);

    // Find pending transactions
    List<WalletTransaction> findByStatusOrderByCreatedAtAsc(WalletTransaction.TransactionStatus status);

    // Find transactions by date range
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY wt.createdAt DESC")
    List<WalletTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    // Get total deposit amount for user
    @Query("SELECT COALESCE(SUM(wt.amount), 0) FROM WalletTransaction wt WHERE wt.userId = :userId " +
            "AND wt.transactionType = 'DEPOSIT' AND wt.status = 'CONFIRMED'")
    BigDecimal getTotalDepositAmountByUser(@Param("userId") String userId);

    // Get transactions by block number range (for scanning)
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.blockNumber BETWEEN :fromBlock AND :toBlock " +
            "ORDER BY wt.blockNumber ASC")
    List<WalletTransaction> findByBlockNumberRange(@Param("fromBlock") Long fromBlock,
                                                   @Param("toBlock") Long toBlock);

    // Check if transaction exists by hash
    boolean existsByTxHash(String txHash);

    // Find transactions that need points crediting
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.transactionType = 'DEPOSIT' " +
            "AND wt.status = 'CONFIRMED' AND wt.pointsCredited IS NULL")
    List<WalletTransaction> findDepositsNeedingPointsCredit();

    // Get paginated transactions for user
    Page<WalletTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Find failed transactions for retry
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.status = 'FAILED' " +
            "AND wt.createdAt > :since ORDER BY wt.createdAt ASC")
    List<WalletTransaction> findFailedTransactionsSince(@Param("since") LocalDateTime since);


    // --- Các method cho WalletTransactionService ---

    Page<WalletTransaction> findByUserIdAndTransactionTypeAndDirection(
            String userId,
            WalletTransaction.TransactionType type,
            WalletTransaction.TransactionDirection direction,
            Pageable pageable
    );

    List<WalletTransaction> findByUserIdAndTransactionTypeAndStatus(
            String userId,
            WalletTransaction.TransactionType type,
            WalletTransaction.TransactionStatus status
    );

    Optional<WalletTransaction> findByIdAndUserIdAndTransactionType(
            Long id,
            String userId,
            WalletTransaction.TransactionType type
    );

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WalletTransaction w " +
            "WHERE w.userId = :userId AND w.transactionType = :type AND w.status = :status")
    BigDecimal sumAmountByUserIdAndTransactionTypeAndStatus(
            @Param("userId") String userId,
            @Param("type") WalletTransaction.TransactionType type,
            @Param("status") WalletTransaction.TransactionStatus status
    );

    long countByUserIdAndTransactionType(
            String userId,
            WalletTransaction.TransactionType type
    );

    Optional<WalletTransaction> findFirstByUserIdAndTransactionTypeOrderByCreatedAtDesc(
            String userId,
            WalletTransaction.TransactionType type
    );

    long countByUserIdAndTransactionTypeAndStatus(
            String userId,
            WalletTransaction.TransactionType type,
            WalletTransaction.TransactionStatus status
    );

    Optional<WalletTransaction> findByTxHashAndUserId(
            String txHash,
            String userId
    );

    // Transaction summary (group by type)
    @Query("SELECT w.transactionType, COUNT(w), COALESCE(SUM(w.amount), 0) " +
            "FROM WalletTransaction w WHERE w.userId = :userId GROUP BY w.transactionType")
    List<Object[]> getTransactionSummaryByUserId(@Param("userId") String userId);

    @Query("SELECT w FROM WalletTransaction w " +
            "WHERE w.userId = :userId AND w.createdAt BETWEEN :startDate AND :endDate")
    List<WalletTransaction> findByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // --- Admin UI specific queries ---
    Page<WalletTransaction> findByTransactionTypeOrderByCreatedAtDesc(
            WalletTransaction.TransactionType type,
            Pageable pageable
    );

    Page<WalletTransaction> findByTransactionTypeAndIsSweptFalseOrderByCreatedAtDesc(
            WalletTransaction.TransactionType type,
            Pageable pageable
    );
}
