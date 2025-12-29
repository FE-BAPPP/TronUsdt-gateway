package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.request.MessageSendRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.dto.response.MessageResponse;
import com.UsdtWallet.UsdtWallet.model.entity.Conversation;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FREELANCER') or hasRole('EMPLOYER')")
public class MessageController {

    private final MessageService messageService;

    /**
     * 💬 Get conversation messages (paginated)
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMessages(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Page<MessageResponse> messages = messageService.getConversationMessages(
            conversationId, currentUser.getId(), page, size);
        
        Long unreadCount = messageService.getUnreadCount(conversationId, currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved", Map.of(
            "messages", messages.getContent(),
            "totalPages", messages.getTotalPages(),
            "totalElements", messages.getTotalElements(),
            "currentPage", messages.getNumber(),
            "unreadCount", unreadCount
        )));
    }

    /**
     * 📤 Send message
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody MessageSendRequest request) {
        
        MessageResponse message = messageService.sendMessage(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Message sent", message));
    }

    /**
     * ✅ Mark message as read
     */
    @PutMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID messageId) {
        
        messageService.markAsRead(messageId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Message marked as read", null));
    }

    /**
     * ✅ Mark all messages in conversation as read
     */
    @PutMapping("/conversation/{conversationId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID conversationId) {
        
        messageService.markAllAsRead(conversationId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("All messages marked as read", null));
    }

    /**
     * 📊 Get unread count
     */
    @GetMapping("/conversation/{conversationId}/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID conversationId) {
        
        Long count = messageService.getUnreadCount(conversationId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", count));
    }

    /**
     * 📋 Get user's conversations
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<Conversation>>> getUserConversations(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<Conversation> conversations = messageService.getUserConversations(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved", conversations));
    }
}