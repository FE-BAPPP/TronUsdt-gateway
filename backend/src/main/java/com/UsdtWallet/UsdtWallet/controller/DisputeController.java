package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.request.DisputeCreateRequest;
import com.UsdtWallet.UsdtWallet.model.dto.request.DisputeResolveRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.dto.response.DisputeResponse;
import com.UsdtWallet.UsdtWallet.model.entity.Dispute;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('FREELANCER') or hasRole('EMPLOYER') or hasRole('ADMIN')")
public class DisputeController {
    
    private final DisputeService disputeService;
    
    /**
     * Create dispute
     */
    @PostMapping
    @PreAuthorize("hasRole('FREELANCER') or hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<DisputeResponse>> createDispute(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody DisputeCreateRequest request) {
        
        try {
            DisputeResponse dispute = disputeService.createDispute(
                    userPrincipal.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("Dispute created successfully", dispute));
        } catch (Exception e) {
            log.error("Failed to create dispute", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.<DisputeResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * Get user's disputes
     */
    @GetMapping("/my-disputes")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Page<DisputeResponse>>> getMyDisputes(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DisputeResponse> disputes = disputeService.getUserDisputes(
                userPrincipal.getId(), pageable);
        
        return ResponseEntity.ok(ApiResponse.success(
                "Disputes retrieved successfully", disputes));
    }
    
    /**
     * Get dispute details
     */
    @GetMapping("/{disputeId}")
    public ResponseEntity<ApiResponse<DisputeResponse>> getDisputeDetails(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID disputeId) {
        
        try {
            DisputeResponse dispute = disputeService.getDisputeDetails(
                    disputeId, userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success("Dispute retrieved successfully", dispute));
        } catch (Exception e) {
            log.error("Failed to get dispute details", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.<DisputeResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * Get all disputes (Admin only)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<DisputeResponse>>> getAllDisputes(
            @RequestParam(required = false) Dispute.DisputeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DisputeResponse> disputes = disputeService.getAllDisputes(status, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(
                "Disputes retrieved successfully", disputes));
    }
    
    /**
     * Resolve dispute (Admin only)
     */
    @PostMapping("/{disputeId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DisputeResponse>> resolveDispute(
            @AuthenticationPrincipal UserPrincipal adminPrincipal,
            @PathVariable UUID disputeId,
            @Valid @RequestBody DisputeResolveRequest request) {
        
        try {
            DisputeResponse dispute = disputeService.resolveDispute(
                    disputeId, adminPrincipal.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("Dispute resolved successfully", dispute));
        } catch (Exception e) {
            log.error("Failed to resolve dispute", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.<DisputeResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * Close dispute (Admin only)
     */
    @PostMapping("/{disputeId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> closeDispute(
            @AuthenticationPrincipal UserPrincipal adminPrincipal,
            @PathVariable UUID disputeId) {
        
        try {
            disputeService.closeDispute(disputeId, adminPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success("Dispute closed successfully", null));
        } catch (Exception e) {
            log.error("Failed to close dispute", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        }
    }
}