package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.entity.Project;
import com.UsdtWallet.UsdtWallet.repository.ProjectRepository;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('FREELANCER') or hasRole('EMPLOYER')")
public class ProjectController {

    private final ProjectRepository projectRepository;

    /**
     * Get employer's projects
     */
    @GetMapping("/employer")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Page<Project>>> getEmployerProjects(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<Project> projects = projectRepository.findByEmployerIdOrderByCreatedAtDesc(
                currentUser.getId(), PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.success("Employer projects fetched successfully", projects));
        } catch (Exception e) {
            log.error("Failed to fetch employer projects", e);
            // ✅ FIX: Explicitly cast to match return type
            return ResponseEntity.badRequest().body(
                ApiResponse.<Page<Project>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get freelancer's projects
     */
    @GetMapping("/freelancer")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<ApiResponse<Page<Project>>> getFreelancerProjects(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<Project> projects = projectRepository.findByFreelancerIdOrderByCreatedAtDesc(
                currentUser.getId(), PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.success("Freelancer projects fetched successfully", projects));
        } catch (Exception e) {
            log.error("Failed to fetch freelancer projects", e);
            // ✅ FIX: Explicitly cast
            return ResponseEntity.badRequest().body(
                ApiResponse.<Page<Project>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Get project details
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Project>> getProjectDetails(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
            
            // Check authorization
            if (!project.getEmployerId().equals(currentUser.getId()) && 
                !project.getFreelancerId().equals(currentUser.getId())) {
                throw new RuntimeException("Not authorized to view this project");
            }
            
            return ResponseEntity.ok(ApiResponse.success("Project details fetched successfully", project));
        } catch (Exception e) {
            log.error("Failed to fetch project details: {}", projectId, e);
            // ✅ FIX: Explicitly cast
            return ResponseEntity.badRequest().body(
                ApiResponse.<Project>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }
}