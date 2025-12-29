package com.UsdtWallet.UsdtWallet.model.dto.request;

import com.UsdtWallet.UsdtWallet.model.entity.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendRequest {
    
    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;
    
    @NotBlank(message = "Message content is required")
    private String content;
    
    @Builder.Default
    private Message.MessageType messageType = Message.MessageType.TEXT;
    
    private String attachmentUrl;
}