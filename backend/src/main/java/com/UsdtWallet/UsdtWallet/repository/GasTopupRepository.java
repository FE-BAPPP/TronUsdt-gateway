package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.GasTopup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GasTopupRepository extends JpaRepository<GasTopup, Long> {

    List<GasTopup> findByStatus(GasTopup.TopupStatus status);

    @Query("SELECT g FROM GasTopup g WHERE g.createdAt >= :since ORDER BY g.createdAt DESC")
    List<GasTopup> findByCreatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT g FROM GasTopup g WHERE g.childIndex = :childIndex ORDER BY g.createdAt DESC")
    List<GasTopup> findByChildIndexOrderByCreatedAtDesc(@Param("childIndex") Integer childIndex);

    @Query("SELECT COUNT(g) FROM GasTopup g WHERE g.status = 'PENDING'")
    Long countPendingTopups();

    @Query("SELECT SUM(g.amountTrx) FROM GasTopup g WHERE g.status = 'CONFIRMED' AND g.createdAt >= :since")
    java.math.BigDecimal getTotalConfirmedAmountSince(@Param("since") LocalDateTime since);
}