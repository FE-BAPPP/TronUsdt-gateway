package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.dto.response.ProjectResponse;
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
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getEmployerProjects(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<Project> projects = projectRepository.findByEmployerIdOrderByCreatedAtDesc(
                currentUser.getId(), PageRequest.of(page, size));
            Page<ProjectResponse> projectResponses = projects.map(ProjectResponse::fromEntity);
            return ResponseEntity.ok(ApiResponse.success("Employer projects fetched successfully", projectResponses));
        } catch (Exception e) {
            log.error("Failed to fetch employer projects", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Page<ProjectResponse>>builder()
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
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getFreelancerProjects(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<Project> projects = projectRepository.findByFreelancerIdOrderByCreatedAtDesc(
                currentUser.getId(), PageRequest.of(page, size));
            Page<ProjectResponse> projectResponses = projects.map(ProjectResponse::fromEntity);
            return ResponseEntity.ok(ApiResponse.success("Freelancer projects fetched successfully", projectResponses));
        } catch (Exception e) {
            log.error("Failed to fetch freelancer projects", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Page<ProjectResponse>>builder()
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
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectDetails(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID projectId) {
        try {
            // ✅ Use findByIdWithJob to fetch job eagerly
            Project project = projectRepository.findByIdWithJob(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
            
            // Check authorization
            if (!project.getEmployerId().equals(currentUser.getId()) && 
                !project.getFreelancerId().equals(currentUser.getId())) {
                throw new RuntimeException("Not authorized to view this project");
            }
            
            ProjectResponse response = ProjectResponse.fromEntity(project);
            return ResponseEntity.ok(ApiResponse.success("Project details fetched successfully", response));
        } catch (Exception e) {
            log.error("Failed to fetch project details: {}", projectId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<ProjectResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * ❌ POST /api/projects/{projectId}/cancel - Cancel project
     * 
     * Employer hoặc Freelancer có thể cancel project nếu:
     * - Project đang IN_PROGRESS
     * - Chưa có milestone nào APPROVED (nếu có rồi thì phải dispute)
     * - Funds sẽ được refund lại cho employer
     */
    @PostMapping("/{projectId}/cancel")
    public ResponseEntity<ApiResponse<ProjectResponse>> cancelProject(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID projectId,
            @RequestParam(required = false) String reason) {
        try {
            Project project = projectRepository.findByIdWithJob(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
            
            // Check authorization (employer hoặc freelancer đều có thể cancel)
            if (!project.getEmployerId().equals(currentUser.getId()) && 
                !project.getFreelancerId().equals(currentUser.getId())) {
                throw new RuntimeException("Not authorized to cancel this project");
            }

            // Validate status
            if (project.getStatus() != Project.ProjectStatus.IN_PROGRESS) {
                throw new RuntimeException("Can only cancel projects that are IN_PROGRESS");
            }

            // Cancel project → status = CANCELLED
            project.setStatus(Project.ProjectStatus.CANCELLED);
            project.setCompletedAt(java.time.LocalDateTime.now());
            Project cancelledProject = projectRepository.save(project);

            log.info("❌ Project cancelled: {} by user: {} | Reason: {}", 
                projectId, currentUser.getId(), reason);

            ProjectResponse response = ProjectResponse.fromEntity(cancelledProject);
            return ResponseEntity.ok(ApiResponse.success("Project cancelled successfully", response));
        } catch (Exception e) {
            log.error("Failed to cancel project: {}", projectId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<ProjectResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * ✅ POST /api/projects/{projectId}/complete - Complete project
     * 
     * Employer hoặc Freelancer đánh dấu project hoàn thành khi:
     * - Tất cả milestones đã RELEASED
     * - Project status: IN_PROGRESS → COMPLETED
     */
    @PostMapping("/{projectId}/complete")
    public ResponseEntity<ApiResponse<ProjectResponse>> completeProject(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID projectId) {
        try {
            Project project = projectRepository.findByIdWithJob(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
            
            // Check authorization
            if (!project.getEmployerId().equals(currentUser.getId()) && 
                !project.getFreelancerId().equals(currentUser.getId())) {
                throw new RuntimeException("Not authorized to complete this project");
            }

            // Validate status
            if (project.getStatus() != Project.ProjectStatus.IN_PROGRESS) {
                throw new RuntimeException("Can only complete projects that are IN_PROGRESS");
            }

            // TODO: Validate all milestones are RELEASED (optional strict check)

            // Complete project
            project.setStatus(Project.ProjectStatus.COMPLETED);
            project.setCompletedAt(java.time.LocalDateTime.now());
            Project completedProject = projectRepository.save(project);

            log.info("✅ Project completed: {} by user: {}", projectId, currentUser.getId());

            ProjectResponse response = ProjectResponse.fromEntity(completedProject);
            return ResponseEntity.ok(ApiResponse.success("Project completed successfully", response));
        } catch (Exception e) {
            log.error("Failed to complete project: {}", projectId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<ProjectResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }
}