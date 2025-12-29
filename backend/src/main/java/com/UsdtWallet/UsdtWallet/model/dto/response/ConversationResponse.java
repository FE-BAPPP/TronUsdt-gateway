package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO cho Conversation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private UUID id;
    private UUID jobId;
    private UUID projectId;
    private String type; // "JOB" or "PROJECT"
    private String title; // Job/Project title
    
    // Thông tin người đối thoại
    private UUID otherPartyId;
    private String otherPartyName;
    private String otherPartyAvatar;
    
    private LocalDateTime createdAt;
    
    // Thông tin tin nhắn cuối (optional)
    private String lastMessage;
    private LocalDateTime lastMessageAt;
}
