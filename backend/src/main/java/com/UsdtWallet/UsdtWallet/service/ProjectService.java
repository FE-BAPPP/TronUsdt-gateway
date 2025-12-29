package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.dto.response.ProjectResponse;
import com.UsdtWallet.UsdtWallet.model.entity.*;
import com.UsdtWallet.UsdtWallet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quản lý Projects
 * 
 * Luồng nghiệp vụ:
 * 1. Tạo project khi employer accept proposal
 * 2. Theo dõi tiến độ project qua milestones
 * 3. Complete project khi tất cả milestones done
 * 4. Cancel hoặc dispute project nếu có vấn đề
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final MilestoneRepository milestoneRepository;
    private final ConversationService conversationService;
    private final NotificationService notificationService;
    private final EmployerProfileService employerProfileService;
    private final FreelancerProfileService freelancerProfileService;

    /**
     * Lấy project theo ID
     */
    public ProjectResponse getProjectById(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Validate user is part of project
        if (!project.getEmployerId().equals(userId) && !project.getFreelancerId().equals(userId)) {
            throw new RuntimeException("You are not authorized to view this project");
        }

        return mapToResponse(project);
    }

    /**
     * Lấy tất cả projects của employer
     */
    public Page<ProjectResponse> getEmployerProjects(UUID employerId, Pageable pageable) {
        Page<Project> projects = projectRepository.findByEmployerIdOrderByCreatedAtDesc(employerId, pageable);
        return projects.map(this::mapToResponse);
    }

    /**
     * Lấy tất cả projects của freelancer
     */
    public Page<ProjectResponse> getFreelancerProjects(UUID freelancerId, Pageable pageable) {
        Page<Project> projects = projectRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancerId, pageable);
        return projects.map(this::mapToResponse);
    }

    /**
     * Lấy projects theo status
     */
    public Page<ProjectResponse> getProjectsByStatus(
            UUID userId, 
            Project.ProjectStatus status, 
            Pageable pageable
    ) {
        Page<Project> projects = projectRepository.findByStatusAndEmployerIdOrFreelancerId(
                status, userId, userId, pageable);
        return projects.map(this::mapToResponse);
    }

    /**
     * Start project (chuyển từ DRAFT sang IN_PROGRESS)
     */
    @Transactional
    public ProjectResponse startProject(UUID projectId, UUID employerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getEmployerId().equals(employerId)) {
            throw new RuntimeException("Only employer can start the project");
        }

        if (project.getStatus() != Project.ProjectStatus.DRAFT) {
            throw new RuntimeException("Can only start DRAFT projects");
        }

        project.setStatus(Project.ProjectStatus.IN_PROGRESS);
        project.setStartedAt(LocalDateTime.now());
        project = projectRepository.save(project);

        log.info("✅ Project started: {}", projectId);

        // Notify freelancer
        notificationService.createNotification(
                project.getFreelancerId(),
                Notification.NotificationType.PROJECT_STARTED,
                "Project Started",
                "Your project has officially started. Good luck!",
                "PROJECT",
                projectId
        );

        return mapToResponse(project);
    }

    /**
     * Complete project (tất cả milestones đã approved)
     */
    @Transactional
    public ProjectResponse completeProject(UUID projectId, UUID employerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getEmployerId().equals(employerId)) {
            throw new RuntimeException("Only employer can complete the project");
        }

        if (project.getStatus() != Project.ProjectStatus.IN_PROGRESS) {
            throw new RuntimeException("Can only complete IN_PROGRESS projects");
        }

        // Validate all milestones are approved/released
        List<Milestone> milestones = milestoneRepository.findByProjectIdOrderBySequenceOrder(projectId);
        boolean allMilestonesCompleted = milestones.stream()
                .allMatch(m -> m.getStatus() == Milestone.MilestoneStatus.APPROVED || 
                              m.getStatus() == Milestone.MilestoneStatus.RELEASED);

        if (!allMilestonesCompleted) {
            throw new RuntimeException("All milestones must be approved before completing project");
        }

        project.setStatus(Project.ProjectStatus.COMPLETED);
        project.setCompletedAt(LocalDateTime.now());
        project = projectRepository.save(project);

        log.info("✅ Project completed: {}", projectId);

        // Update profiles
        employerProfileService.decrementActiveProjects(employerId);
        freelancerProfileService.incrementJobsCompleted(project.getFreelancerId());

        // Notify freelancer
        notificationService.createNotification(
                project.getFreelancerId(),
                Notification.NotificationType.SYSTEM_ALERT,
                "Project Completed",
                "Congratulations! Your project has been marked as completed.",
                "PROJECT",
                projectId
        );

        return mapToResponse(project);
    }

    /**
     * Cancel project
     */
    @Transactional
    public ProjectResponse cancelProject(UUID projectId, UUID userId, String reason) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Either party can cancel
        if (!project.getEmployerId().equals(userId) && !project.getFreelancerId().equals(userId)) {
            throw new RuntimeException("Only project participants can cancel");
        }

        if (project.getStatus() == Project.ProjectStatus.COMPLETED || 
            project.getStatus() == Project.ProjectStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel project in current status");
        }

        project.setStatus(Project.ProjectStatus.CANCELLED);
        project.setCompletedAt(LocalDateTime.now());
        project = projectRepository.save(project);

        log.info("❌ Project cancelled: {} by {}, reason: {}", projectId, userId, reason);

        // Update profiles
        employerProfileService.decrementActiveProjects(project.getEmployerId());

        // Notify other party
        UUID otherPartyId = project.getEmployerId().equals(userId) 
                ? project.getFreelancerId() 
                : project.getEmployerId();

        notificationService.createNotification(
                otherPartyId,
                Notification.NotificationType.SYSTEM_ALERT,
                "Project Cancelled",
                "The project has been cancelled. Reason: " + reason,
                "PROJECT",
                projectId
        );

        return mapToResponse(project);
    }

    /**
     * Get project completion percentage
     */
    public int getProjectCompletionPercentage(UUID projectId) {
        List<Milestone> milestones = milestoneRepository.findByProjectIdOrderBySequenceOrder(projectId);
        
        if (milestones.isEmpty()) {
            return 0;
        }

        long completedCount = milestones.stream()
                .filter(m -> m.getStatus() == Milestone.MilestoneStatus.APPROVED || 
                           m.getStatus() == Milestone.MilestoneStatus.RELEASED)
                .count();

        return (int) ((completedCount * 100) / milestones.size());
    }

    /**
     * Get project statistics
     */
    public ProjectStatistics getProjectStatistics(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<Milestone> milestones = milestoneRepository.findByProjectIdOrderBySequenceOrder(projectId);

        long totalMilestones = milestones.size();
        long completedMilestones = milestones.stream()
                .filter(m -> m.getStatus() == Milestone.MilestoneStatus.APPROVED || 
                           m.getStatus() == Milestone.MilestoneStatus.RELEASED)
                .count();
        long inProgressMilestones = milestones.stream()
                .filter(m -> m.getStatus() == Milestone.MilestoneStatus.IN_PROGRESS)
                .count();
        long pendingMilestones = milestones.stream()
                .filter(m -> m.getStatus() == Milestone.MilestoneStatus.PENDING)
                .count();

        BigDecimal totalAmount = project.getAgreedAmount();
        BigDecimal paidAmount = milestones.stream()
                .filter(m -> m.getStatus() == Milestone.MilestoneStatus.RELEASED)
                .map(Milestone::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int completionPercentage = getProjectCompletionPercentage(projectId);

        return ProjectStatistics.builder()
                .projectId(projectId)
                .status(project.getStatus().name())
                .totalMilestones(totalMilestones)
                .completedMilestones(completedMilestones)
                .inProgressMilestones(inProgressMilestones)
                .pendingMilestones(pendingMilestones)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .remainingAmount(totalAmount.subtract(paidAmount))
                .completionPercentage(completionPercentage)
                .startedAt(project.getStartedAt())
                .completedAt(project.getCompletedAt())
                .build();
    }

    /**
     * Map entity to response
     */
    private ProjectResponse mapToResponse(Project project) {
        Job job = jobRepository.findById(project.getJobId()).orElse(null);
        User employer = userRepository.findById(project.getEmployerId()).orElse(null);
        User freelancer = userRepository.findById(project.getFreelancerId()).orElse(null);

        // Get milestones count
        long totalMilestones = milestoneRepository.countByProjectId(project.getId());
        long completedMilestones = milestoneRepository.countByProjectIdAndStatus(
                project.getId(), Milestone.MilestoneStatus.APPROVED);

        return ProjectResponse.builder()
                .id(project.getId())
                .jobId(project.getJobId())
                .jobTitle(job != null ? job.getTitle() : null)
                .employerId(project.getEmployerId())
                .employerName(employer != null ? employer.getFullName() : null)
                .employerAvatar(employer != null ? employer.getAvatar() : null)
                .freelancerId(project.getFreelancerId())
                .freelancerName(freelancer != null ? freelancer.getFullName() : null)
                .freelancerAvatar(freelancer != null ? freelancer.getAvatar() : null)
                .agreedAmount(project.getAgreedAmount())
                .currency(project.getCurrency())
                .status(project.getStatus().name())
                .totalMilestones(totalMilestones)
                .completedMilestones(completedMilestones)
                .completionPercentage(getProjectCompletionPercentage(project.getId()))
                .startedAt(project.getStartedAt())
                .completedAt(project.getCompletedAt())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    /**
     * Inner class for project statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class ProjectStatistics {
        private UUID projectId;
        private String status;
        private long totalMilestones;
        private long completedMilestones;
        private long inProgressMilestones;
        private long pendingMilestones;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal remainingAmount;
        private int completionPercentage;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
    }
}
