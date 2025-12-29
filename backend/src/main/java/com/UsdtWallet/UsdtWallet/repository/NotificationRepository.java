package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    List<Notification> findByUserIdAndIsReadFalse(UUID userId);
    
    Long countByUserIdAndIsReadFalse(UUID userId);
    
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
    
    // Find notifications by entity
    List<Notification> findByEntityTypeAndEntityId(String entityType, UUID entityId);
}