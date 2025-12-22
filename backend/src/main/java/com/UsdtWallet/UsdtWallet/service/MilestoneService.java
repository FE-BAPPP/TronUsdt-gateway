package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.entity.*;
import com.UsdtWallet.UsdtWallet.repository.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final EscrowRepository escrowRepository;
    private final PointsService pointsService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Get milestones for a project
     */
    public List<Milestone> getProjectMilestones(UUID projectId, UUID userId) {
        // Validate user has access to this project
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getEmployerId().equals(userId) && !project.getFreelancerId().equals(userId)) {
            throw new RuntimeException("Not authorized to view project milestones");
        }
        
        return milestoneRepository.findByProjectIdOrderByCreatedAt(projectId);
    }

    /**
     * Release milestone payment (Employer action)
     */
    @Transactional
    public Milestone releaseMilestone(UUID milestoneId, UUID employerId) {
        // 1. Get milestone
        Milestone milestone = milestoneRepository.findById(milestoneId)
            .orElseThrow(() -> new RuntimeException("Milestone not found"));
        
        if (milestone.getStatus() != Milestone.MilestoneStatus.PENDING) {
            throw new RuntimeException("Milestone already released");
        }
        
        // 2. Validate employer owns the project
        Project project = projectRepository.findById(milestone.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getEmployerId().equals(employerId)) {
            throw new RuntimeException("Not authorized to release this milestone");
        }
        
        // 3. Get escrow
        Escrow escrow = escrowRepository.findByProjectId(project.getId())
            .orElseThrow(() -> new RuntimeException("Escrow not found for project"));
        
        if (escrow.getStatus() != Escrow.EscrowStatus.LOCKED) {
            throw new RuntimeException("Escrow is not in LOCKED state");
        }
        
        // 4. Release funds via PointsService
        boolean released = pointsService.releaseEscrowToFreelancer(
            employerId,
            project.getFreelancerId(),
            milestone.getAmount(),
            project.getId().toString()
        );
        
        if (!released) {
            throw new RuntimeException("Failed to release escrow funds");
        }
        
        // 5. Update milestone status
        milestone.setStatus(Milestone.MilestoneStatus.RELEASED);
        milestone.setReleasedAt(LocalDateTime.now());
        milestoneRepository.save(milestone);
        
        // 6. Check if all milestones released → complete project
        long pendingMilestones = milestoneRepository.countByProjectIdAndStatus(
            project.getId(), Milestone.MilestoneStatus.PENDING);
        
        if (pendingMilestones == 0) {
            // All milestones released → Complete project
            project.setStatus(Project.ProjectStatus.COMPLETED);
            project.setCompletedAt(LocalDateTime.now());
            projectRepository.save(project);
            
            // Update escrow status
            escrow.setStatus(Escrow.EscrowStatus.RELEASED);
            escrow.setReleasedAt(LocalDateTime.now());
            escrowRepository.save(escrow);
            
            log.info("✅ Project completed: {}", project.getId());
        }
        
        log.info("✅ Milestone released: {} for project: {}, amount: {}", 
            milestoneId, project.getId(), milestone.getAmount());
        
        return milestone;
    }

    /**
     * Get milestone statistics for project
     */
    public MilestoneStats getProjectMilestoneStats(UUID projectId) {
        List<Milestone> milestones = milestoneRepository.findByProjectIdOrderByCreatedAt(projectId);
        
        BigDecimal totalAmount = milestones.stream()
            .map(Milestone::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal releasedAmount = milestones.stream()
            .filter(m -> m.getStatus() == Milestone.MilestoneStatus.RELEASED)
            .map(Milestone::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long totalCount = milestones.size();
        long releasedCount = milestones.stream()
            .filter(m -> m.getStatus() == Milestone.MilestoneStatus.RELEASED)
            .count();
        
        return MilestoneStats.builder()
            .totalMilestones(totalCount)
            .releasedMilestones(releasedCount)
            .pendingMilestones(totalCount - releasedCount)
            .totalAmount(totalAmount)
            .releasedAmount(releasedAmount)
            .pendingAmount(totalAmount.subtract(releasedAmount))
            .build();
    }
    
    /**
     * ✅ Milestone statistics DTO (inner static class)
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class MilestoneStats {
        private long totalMilestones;
        private long releasedMilestones;
        private long pendingMilestones;
        private BigDecimal totalAmount;
        private BigDecimal releasedAmount;
        private BigDecimal pendingAmount;
    }
}