package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    
    Optional<Conversation> findByJobId(UUID jobId);
    Optional<Conversation> findByProjectId(UUID projectId);
    
    @Query("SELECT c FROM Conversation c WHERE c.projectId IN " +
           "(SELECT p.id FROM Project p WHERE p.employerId = :userId OR p.freelancerId = :userId)")
    List<Conversation> findUserConversations(@Param("userId") UUID userId);
}