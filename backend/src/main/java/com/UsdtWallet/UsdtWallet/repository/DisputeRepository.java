package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {
    
    Page<Dispute> findByStatus(Dispute.DisputeStatus status, Pageable pageable);
    
    Page<Dispute> findByRaisedBy(UUID raisedBy, Pageable pageable);
    
    boolean existsByProjectIdAndStatusNot(UUID projectId, Dispute.DisputeStatus status);
    
    // Statistics
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}