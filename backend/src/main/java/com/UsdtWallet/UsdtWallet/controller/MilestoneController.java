package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.entity.Milestone;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.MilestoneService;
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
            // ✅ FIX: Explicitly cast to match return type
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
            // ✅ FIX: Explicitly cast
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
            // ✅ FIX: Explicitly cast
            return ResponseEntity.badRequest().body(
                ApiResponse.<MilestoneService.MilestoneStats>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }
}