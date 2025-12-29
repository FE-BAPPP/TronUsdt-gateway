package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {
    
    List<Milestone> findByProjectIdOrderByCreatedAt(UUID projectId);
    
    List<Milestone> findByProjectIdOrderBySequenceOrder(UUID projectId);
    
    long countByProjectIdAndStatus(UUID projectId, Milestone.MilestoneStatus status);
    
    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM Milestone m WHERE m.projectId = :projectId AND m.status = :status")
    BigDecimal getTotalAmountByProjectAndStatus(@Param("projectId") UUID projectId, 
                                                @Param("status") Milestone.MilestoneStatus status);
    
    // 🆕 Thêm method này
    @Query("SELECT COALESCE(MAX(m.sequenceOrder), 0) FROM Milestone m WHERE m.projectId = :projectId")
    Optional<Integer> findMaxSequenceOrderByProjectId(@Param("projectId") UUID projectId);
    
    // 🆕 Thêm method này
    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM Milestone m WHERE m.projectId = :projectId")
    Optional<BigDecimal> sumAmountByProjectId(@Param("projectId") UUID projectId);
    
    // Count milestones by project
    long countByProjectId(UUID projectId);
}