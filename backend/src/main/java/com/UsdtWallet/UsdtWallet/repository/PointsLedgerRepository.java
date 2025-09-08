package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.PointsLedger;
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
public interface PointsLedgerRepository extends JpaRepository<PointsLedger, String> {

    // Find user's points history
    List<PointsLedger> findByUserIdOrderByCreatedAtDesc(String userId);

    // Get paginated points history for user
    Page<PointsLedger> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Get user's current balance (latest record)
    @Query("SELECT pl FROM PointsLedger pl WHERE pl.userId = :userId " +
           "ORDER BY pl.createdAt DESC LIMIT 1")
    PointsLedger findLatestByUserId(@Param("userId") String userId);

    // Calculate user's current balance
    @Query("SELECT COALESCE(pl.balanceAfter, 0) FROM PointsLedger pl WHERE pl.userId = :userId " +
           "AND pl.status = 'COMPLETED' ORDER BY pl.createdAt DESC LIMIT 1")
    BigDecimal getCurrentBalance(@Param("userId") String userId);

    // Find transactions by type
    List<PointsLedger> findByUserIdAndTransactionTypeOrderByCreatedAtDesc(
        String userId, PointsLedger.PointsTransactionType transactionType);

    // Find P2P transactions between users
    @Query("SELECT pl FROM PointsLedger pl WHERE " +
           "(pl.fromUserId = :userId OR pl.toUserId = :userId) " +
           "AND pl.transactionType IN ('P2P_SEND', 'P2P_RECEIVE') " +
           "ORDER BY pl.createdAt DESC")
    List<PointsLedger> findP2PTransactionsByUserId(@Param("userId") String userId);

    // Find pending transactions
    List<PointsLedger> findByStatusOrderByCreatedAtAsc(PointsLedger.PointsTransactionStatus status);

    // Get total credits for user
    @Query("SELECT COALESCE(SUM(pl.amount), 0) FROM PointsLedger pl WHERE pl.userId = :userId " +
           "AND pl.transactionType = 'DEPOSIT_CREDIT' AND pl.status = 'COMPLETED'")
    BigDecimal getTotalDepositCredits(@Param("userId") String userId);

    // Get total P2P sent for user
    @Query("SELECT COALESCE(SUM(pl.amount), 0) FROM PointsLedger pl WHERE pl.userId = :userId " +
           "AND pl.transactionType = 'P2P_SEND' AND pl.status = 'COMPLETED'")
    BigDecimal getTotalP2PSent(@Param("userId") String userId);

    // Get total P2P received for user
    @Query("SELECT COALESCE(SUM(pl.amount), 0) FROM PointsLedger pl WHERE pl.userId = :userId " +
           "AND pl.transactionType = 'P2P_RECEIVE' AND pl.status = 'COMPLETED'")
    BigDecimal getTotalP2PReceived(@Param("userId") String userId);

    // Find transactions by reference ID (for P2P matching)
    List<PointsLedger> findByReferenceId(String referenceId);

    // Find transactions by date range
    @Query("SELECT pl FROM PointsLedger pl WHERE pl.userId = :userId " +
           "AND pl.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY pl.createdAt DESC")
    List<PointsLedger> findByUserIdAndDateRange(@Param("userId") String userId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    // Check if transaction already exists for wallet transaction
    boolean existsByTransactionId(String transactionId);

    Optional<PointsLedger> findFirstByTransactionId(String transactionId);

    @Query("SELECT COALESCE(SUM(pl.amount), 0) FROM PointsLedger pl WHERE pl.userId = :userId " +
           "AND pl.transactionType = 'WITHDRAWAL_DEBIT' AND pl.status = 'PENDING'")
    BigDecimal getTotalPendingWithdrawalLocks(@Param("userId") String userId);
}
