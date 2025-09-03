package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by user ID
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    /**
     * Find audit logs by action
     */
    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    /**
     * Find audit logs by entity type
     */
    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);

    /**
     * Find audit logs by date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   Pageable pageable);

    /**
     * Find security events
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action LIKE 'SECURITY_%' ORDER BY a.timestamp DESC")
    List<AuditLog> findSecurityEvents();

    /**
     * Count logs by action in date range
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action AND a.timestamp >= :since")
    long countByActionSince(@Param("action") String action, @Param("since") LocalDateTime since);
}
