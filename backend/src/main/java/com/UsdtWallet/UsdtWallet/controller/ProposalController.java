package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.request.ProposalCreateRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.dto.response.ProposalResponse;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.ProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('FREELANCER') or hasRole('EMPLOYER')")
public class ProposalController {

    private final ProposalService proposalService;

    /**
     * Freelancer submits a proposal
     */
    @PostMapping
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<ApiResponse<ProposalResponse>> submitProposal(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ProposalCreateRequest request) {
        
        try {
            ProposalResponse response = proposalService.submitProposal(
                userPrincipal.getId(), request);
            
            return ResponseEntity.ok(ApiResponse.<ProposalResponse>builder()
                .success(true)
                .message("Proposal submitted successfully")
                .data(response)
                .build());
        } catch (RuntimeException e) {
            log.error("Failed to submit proposal: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<ProposalResponse>builder()
                .success(false)
                .message(e.getMessage())
                .build());
        }
    }

    /**
     * Get proposals for a job (Employer view)
     */
    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Page<ProposalResponse>>> getProposalsForJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<ProposalResponse> proposals = proposalService.getProposalsForJob(
                jobId, userPrincipal.getId(), pageable);
            
            return ResponseEntity.ok(ApiResponse.<Page<ProposalResponse>>builder()
                .success(true)
                .data(proposals)
                .build());
        } catch (RuntimeException e) {
            log.error("Failed to get proposals for job {}: {}", jobId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<Page<ProposalResponse>>builder()
                .success(false)
                .message(e.getMessage())
                .build());
        }
    }

    /**
     * Get freelancer's own proposals
     */
    @GetMapping("/my-proposals")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<ApiResponse<Page<ProposalResponse>>> getMyProposals(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProposalResponse> proposals = proposalService.getFreelancerProposals(
            userPrincipal.getId(), pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<ProposalResponse>>builder()
            .success(true)
            .data(proposals)
            .build());
    }

    /**
     * Get proposal details
     */
    @GetMapping("/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalResponse>> getProposal(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID proposalId) {
        
        try {
            ProposalResponse proposal = proposalService.getProposalById(
                proposalId, userPrincipal.getId());
            
            return ResponseEntity.ok(ApiResponse.<ProposalResponse>builder()
                .success(true)
                .data(proposal)
                .build());
        } catch (RuntimeException e) {
            log.error("Failed to get proposal {}: {}", proposalId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<ProposalResponse>builder()
                .success(false)
                .message(e.getMessage())
                .build());
        }
    }

    /**
     * Award a proposal (Employer)
     */
    @PostMapping("/{proposalId}/award")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<ProposalResponse>> awardProposal(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID proposalId) {
        
        try {
            ProposalResponse response = proposalService.awardProposal(
                proposalId, userPrincipal.getId());
            
            return ResponseEntity.ok(ApiResponse.<ProposalResponse>builder()
                .success(true)
                .message("Proposal awarded successfully")
                .data(response)
                .build());
        } catch (RuntimeException e) {
            log.error("Failed to award proposal {}: {}", proposalId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<ProposalResponse>builder()
                .success(false)
                .message(e.getMessage())
                .build());
        }
    }

    /**
     * 🔄 PUT /api/proposals/{proposalId} - Freelancer cập nhật proposal của mình
     * 
     * Chỉ có thể update khi proposal vẫn PENDING (chưa được xét)
     */
    @PutMapping("/{proposalId}")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<ApiResponse<ProposalResponse>> updateProposal(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID proposalId,
            @Valid @RequestBody ProposalCreateRequest request) {
        
        try {
            ProposalResponse response = proposalService.updateProposal(
                proposalId, userPrincipal.getId(), request);
            
            return ResponseEntity.ok(ApiResponse.<ProposalResponse>builder()
                .success(true)
                .message("Proposal updated successfully")
                .data(response)
                .build());
        } catch (RuntimeException e) {
            log.error("Failed to update proposal {}: {}", proposalId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<ProposalResponse>builder()
                .success(false)
                .message(e.getMessage())
                .build());
        }
    }

    /**
     * ❌ POST /api/proposals/{proposalId}/withdraw - Freelancer rút proposal
     * 
     * Freelancer có thể withdraw proposal nếu:
     * - Proposal vẫn PENDING (chưa được award)
     * - Không muốn làm job nữa
     */
    @PostMapping("/{proposalId}/withdraw")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<ApiResponse<ProposalResponse>> withdrawProposal(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID proposalId) {
        
        try {
            ProposalResponse response = proposalService.withdrawProposal(
                proposalId, userPrincipal.getId());
            
            return ResponseEntity.ok(ApiResponse.<ProposalResponse>builder()
                .success(true)
                .message("Proposal withdrawn successfully")
                .data(response)
                .build());
        } catch (RuntimeException e) {
            log.error("Failed to withdraw proposal {}: {}", proposalId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<ProposalResponse>builder()
                .success(false)
                .message(e.getMessage())
                .build());
        }
    }
}