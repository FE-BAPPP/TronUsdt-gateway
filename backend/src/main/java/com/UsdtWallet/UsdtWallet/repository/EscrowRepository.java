package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.Escrow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EscrowRepository extends JpaRepository<Escrow, UUID> {
    
    Optional<Escrow> findByProjectId(UUID projectId);
    
    Optional<Escrow> findByMilestoneId(UUID milestoneId);
    
    boolean existsByMilestoneId(UUID milestoneId);
    
    List<Escrow> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    
    List<Escrow> findByEmployerIdOrderByCreatedAtDesc(UUID employerId);
    
    // Get total locked amount for employer
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Escrow e WHERE e.employerId = :employerId AND e.status = 'LOCKED'")
    BigDecimal getTotalLockedForEmployer(@Param("employerId") UUID employerId);
    
    // Get total locked amount for project
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Escrow e WHERE e.projectId = :projectId AND e.status = 'LOCKED'")
    BigDecimal findTotalLockedAmountByProject(@Param("projectId") UUID projectId);
}