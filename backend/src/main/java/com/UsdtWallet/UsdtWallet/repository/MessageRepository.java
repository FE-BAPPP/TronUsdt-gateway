package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);
    
    List<Message> findTop50ByConversationIdOrderByCreatedAtDesc(UUID conversationId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.senderId != :userId AND m.isRead = false")
    Long countUnreadMessages(@Param("conversationId") UUID conversationId, 
                            @Param("userId") UUID userId);
    
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.senderId != :userId AND m.isRead = false")
    List<Message> findUnreadMessages(@Param("conversationId") UUID conversationId, 
                                    @Param("userId") UUID userId);
}