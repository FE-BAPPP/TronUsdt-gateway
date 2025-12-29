package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.entity.Conversation;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FREELANCER') or hasRole('EMPLOYER')")
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * 💬 Get conversation by project ID
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<Conversation>> getConversationByProject(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID projectId) {
        
        Conversation conversation = conversationService.getConversationByProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved", conversation));
    }

    /**
     * 💬 Get conversation by job ID
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<Conversation>> getConversationByJob(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID jobId) {
        
        Conversation conversation = conversationService.getConversationByJobId(jobId);
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved", conversation));
    }

    /**
     * ✅ Check if conversation exists for project
     */
    @GetMapping("/project/{projectId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkConversationExists(
            @PathVariable UUID projectId) {
        
        boolean exists = conversationService.conversationExistsForProject(projectId);
        return ResponseEntity.ok(ApiResponse.success("Checked", exists));
    }
}
