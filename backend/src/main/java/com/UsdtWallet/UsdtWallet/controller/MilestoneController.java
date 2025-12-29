package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.request.*;
import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.entity.Milestone;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.MilestoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('FREELANCER') or hasRole('EMPLOYER')")
public class MilestoneController {

    private final MilestoneService milestoneService;

    /**
     * Get milestones for a project
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<Milestone>>> getProjectMilestones(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID projectId) {
        try {
            List<Milestone> milestones = milestoneService.getProjectMilestones(projectId, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Milestones fetched successfully", milestones));
        } catch (Exception e) {
            log.error("Failed to fetch milestones for project: {}", projectId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<List<Milestone>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Release milestone payment (Employer only)
     */
    @PostMapping("/{milestoneId}/release")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Milestone>> releaseMilestone(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID milestoneId) {
        try {
            Milestone milestone = milestoneService.releaseMilestone(milestoneId, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Milestone payment released successfully", milestone));
        } catch (Exception e) {
            log.error("Failed to release milestone: {}", milestoneId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Milestone>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get milestone statistics for project
     */
    @GetMapping("/project/{projectId}/stats")
    public ResponseEntity<ApiResponse<MilestoneService.MilestoneStats>> getProjectMilestoneStats(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID projectId) {
        try {
            MilestoneService.MilestoneStats stats = milestoneService.getProjectMilestoneStats(projectId);
            return ResponseEntity.ok(ApiResponse.success("Milestone stats fetched successfully", stats));
        } catch (Exception e) {
            log.error("Failed to fetch milestone stats for project: {}", projectId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<MilestoneService.MilestoneStats>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * 🆕 CREATE MILESTONE (Employer only)
     */
    @PostMapping("/project/{projectId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Milestone>> createMilestone(
        @AuthenticationPrincipal UserPrincipal currentUser,
        @PathVariable UUID projectId,
        @Valid @RequestBody MilestoneCreateRequest request) {
        
        try {
            Milestone milestone = milestoneService.createMilestone(
                currentUser.getId(), 
                projectId, 
                request
            );
            
            return ResponseEntity.ok(
                ApiResponse.success("Milestone created successfully", milestone)
            );
        } catch (Exception e) {
            log.error("Failed to create milestone: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.<Milestone>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * 🆕 UPDATE MILESTONE (Employer only, before funded)
     */
    @PutMapping("/{milestoneId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Milestone>> updateMilestone(
        @AuthenticationPrincipal UserPrincipal currentUser,
        @PathVariable UUID milestoneId,
        @Valid @RequestBody MilestoneUpdateRequest request) {
        
        try {
            Milestone updated = milestoneService.updateMilestone(
                currentUser.getId(), 
                milestoneId, 
                request
            );
            
            return ResponseEntity.ok(
                ApiResponse.success("Milestone updated", updated)
            );
        } catch (Exception e) {
            log.error("Failed to update milestone: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.<Milestone>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * 🆕 DELETE MILESTONE (Employer only, before funded)
     */
    @DeleteMapping("/{milestoneId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(
        @AuthenticationPrincipal UserPrincipal currentUser,
        @PathVariable UUID milestoneId) {
        
        try {
            milestoneService.deleteMilestone(currentUser.getId(), milestoneId);
            
            return ResponseEntity.ok(
                ApiResponse.success("Milestone deleted", null)
            );
        } catch (Exception e) {
            log.error("Failed to delete milestone: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * 🆕 START MILESTONE (Freelancer) - Changes status PENDING → IN_PROGRESS
     */
    @PostMapping("/{milestoneId}/start")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<ApiResponse<Milestone>> startMilestone(
        @AuthenticationPrincipal UserPrincipal currentUser,
        @PathVariable UUID milestoneId) {
        
        try {
            Milestone started = milestoneService.startMilestone(
                currentUser.getId(), 
                milestoneId
            );
            
            return ResponseEntity.ok(
                ApiResponse.success("Milestone started. You can now begin working.", started)
            );
        } catch (Exception e) {
            log.error("Failed to start milestone: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.<Milestone>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * 🆕 SUBMIT MILESTONE FOR REVIEW (Freelancer)
     */
    @PostMapping("/{milestoneId}/submit")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<ApiResponse<Milestone>> submitMilestone(
        @AuthenticationPrincipal UserPrincipal currentUser,
        @PathVariable UUID milestoneId,
        @Valid @RequestBody MilestoneSubmitRequest request) {
        
        try {
            Milestone submitted = milestoneService.submitMilestoneForReview(
                currentUser.getId(), 
                milestoneId,
                request.getDeliverables(),
                request.getNotes()
            );
            
            return ResponseEntity.ok(
                ApiResponse.success("Milestone submitted for review", submitted)
            );
        } catch (Exception e) {
            log.error("Failed to submit milestone: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.<Milestone>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * 🆕 APPROVE MILESTONE (Employer)
     */
    @PostMapping("/{milestoneId}/approve")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Milestone>> approveMilestone(
        @AuthenticationPrincipal UserPrincipal currentUser,
        @PathVariable UUID milestoneId) {
        
        try {
            Milestone approved = milestoneService.approveMilestone(
                currentUser.getId(), 
                milestoneId
            );
            
            return ResponseEntity.ok(
                ApiResponse.success("Milestone approved", approved)
            );
        } catch (Exception e) {
            log.error("Failed to approve milestone: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.<Milestone>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * 🆕 REJECT MILESTONE (Employer)
     */
    @PostMapping("/{milestoneId}/reject")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Milestone>> rejectMilestone(
        @AuthenticationPrincipal UserPrincipal currentUser,
        @PathVariable UUID milestoneId,
        @Valid @RequestBody MilestoneRejectRequest request) {
        
        try {
            Milestone rejected = milestoneService.rejectMilestone(
                currentUser.getId(), 
                milestoneId,
                request.getReason()
            );
            
            return ResponseEntity.ok(
                ApiResponse.success("Milestone rejected", rejected)
            );
        } catch (Exception e) {
            log.error("Failed to reject milestone: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.<Milestone>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }
}