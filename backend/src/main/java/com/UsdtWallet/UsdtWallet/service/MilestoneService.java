package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.dto.*;
import com.UsdtWallet.UsdtWallet.model.dto.request.*;
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
    private final PointsService pointsService;
    private final FreelancerProfileService freelancerProfileService;
    private final EmployerProfileService employerProfileService;
    
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
        
        // 3. Release funds via PointsService
        boolean released = pointsService.releaseEscrowToFreelancer(
            employerId,
            project.getFreelancerId(),
            milestone.getAmount(),
            project.getId().toString()
        );
        
        if (!released) {
            throw new RuntimeException("Failed to release escrow funds");
        }
        
        // 4. Update milestone status
        milestone.setStatus(Milestone.MilestoneStatus.RELEASED);
        milestone.setReleasedAt(LocalDateTime.now());
        milestoneRepository.save(milestone);
        
        // 5. Update freelancer profile stats: Add to total earnings
        freelancerProfileService.addToTotalEarnings(project.getFreelancerId(), milestone.getAmount());
        log.debug("💰 Added {} to freelancer {} total_earnings", milestone.getAmount(), project.getFreelancerId());
        
        // 6. Check if all milestones released → complete project
        long pendingMilestones = milestoneRepository.countByProjectIdAndStatus(
            project.getId(), Milestone.MilestoneStatus.PENDING);
        
        if (pendingMilestones == 0) {
            // All milestones released → Complete project
            project.setStatus(Project.ProjectStatus.COMPLETED);
            project.setCompletedAt(LocalDateTime.now());
            projectRepository.save(project);
            
            // Update employer stats
            employerProfileService.decrementActiveProjects(project.getEmployerId());
            employerProfileService.addToTotalSpent(project.getEmployerId(), project.getAgreedAmount());
            log.debug("📉 Decremented active_projects and added {} to total_spent for employer: {}", 
                project.getAgreedAmount(), project.getEmployerId());
            
            // Update freelancer stats: Increment jobs_completed
            freelancerProfileService.incrementJobsCompleted(project.getFreelancerId());
            log.debug("📈 Incremented jobs_completed for freelancer: {}", project.getFreelancerId());
            
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
    
    /**
     * 🆕 CREATE MILESTONE (Employer)
     */
    @Transactional
    public Milestone createMilestone(UUID employerId, UUID projectId, MilestoneCreateRequest request) {
        
        // 1. Validate project exists and employer owns it
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getEmployerId().equals(employerId)) {
            throw new RuntimeException("You are not authorized to create milestones for this project");
        }
        
        if (project.getStatus() != Project.ProjectStatus.IN_PROGRESS) {
            throw new RuntimeException("Can only create milestones for IN_PROGRESS projects");
        }
        
        // 2. Get next sequence order
        int nextOrder = milestoneRepository.findMaxSequenceOrderByProjectId(projectId)
            .orElse(0) + 1;
        
        // 3. Validate sum of milestone amounts <= project agreed amount
        BigDecimal totalMilestoneAmount = milestoneRepository
            .sumAmountByProjectId(projectId)
            .orElse(BigDecimal.ZERO)
            .add(request.getAmount());
        
        if (totalMilestoneAmount.compareTo(project.getAgreedAmount()) > 0) {
            throw new RuntimeException("Total milestone amount exceeds project budget");
        }
        
        // 4. Create milestone
        Milestone milestone = Milestone.builder()
            .projectId(projectId)
            .title(request.getTitle())
            .description(request.getDescription())
            .amount(request.getAmount())
            .currency(project.getCurrency())
            .sequenceOrder(nextOrder)
            .status(Milestone.MilestoneStatus.PENDING)
            .dueDate(request.getDueDate())
            .build();
        
        Milestone saved = milestoneRepository.save(milestone);
        log.info("✅ Milestone created: {} for project: {}", saved.getId(), projectId);
        
        return saved;
    }
    
    /**
     * 🆕 UPDATE MILESTONE (Employer, before funded)
     */
    @Transactional
    public Milestone updateMilestone(UUID employerId, UUID milestoneId, MilestoneUpdateRequest request) {
        
        Milestone milestone = milestoneRepository.findById(milestoneId)
            .orElseThrow(() -> new RuntimeException("Milestone not found"));
        
        Project project = projectRepository.findById(milestone.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getEmployerId().equals(employerId)) {
            throw new RuntimeException("Not authorized");
        }
        
        // ⚠️ Can only update PENDING milestones
        if (milestone.getStatus() != Milestone.MilestoneStatus.PENDING) {
            throw new RuntimeException("Can only update PENDING milestones");
        }
        
        // Update fields
        if (request.getTitle() != null) {
            milestone.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            milestone.setDescription(request.getDescription());
        }
        if (request.getAmount() != null) {
            // Validate new total
            BigDecimal newTotal = milestoneRepository
                .sumAmountByProjectId(project.getId())
                .orElse(BigDecimal.ZERO)
                .subtract(milestone.getAmount())
                .add(request.getAmount());
            
            if (newTotal.compareTo(project.getAgreedAmount()) > 0) {
                throw new RuntimeException("Updated amount exceeds project budget");
            }
            
            milestone.setAmount(request.getAmount());
        }
        if (request.getDueDate() != null) {
            milestone.setDueDate(request.getDueDate());
        }
        
        return milestoneRepository.save(milestone);
    }
    
    /**
     * 🆕 DELETE MILESTONE (Employer, before funded)
     */
    @Transactional
    public void deleteMilestone(UUID employerId, UUID milestoneId) {
        
        Milestone milestone = milestoneRepository.findById(milestoneId)
            .orElseThrow(() -> new RuntimeException("Milestone not found"));
        
        Project project = projectRepository.findById(milestone.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getEmployerId().equals(employerId)) {
            throw new RuntimeException("Not authorized");
        }
        
        // ⚠️ Can only delete PENDING milestones
        if (milestone.getStatus() != Milestone.MilestoneStatus.PENDING) {
            throw new RuntimeException("Can only delete PENDING milestones");
        }
        
        milestoneRepository.delete(milestone);
        log.info("✅ Milestone deleted: {}", milestoneId);
    }
    
    // ========================================================================
    // LUỒNG MILESTONE: PHẢI TUẦN TỰ - KHÔNG ĐƯỢC SKIP!
    // ========================================================================
    // QUY TẮC: Phải hoàn thành milestone 1 trước khi bắt đầu milestone 2
    // PENDING → IN_PROGRESS → SUBMITTED → APPROVED → RELEASED
    
    /**
     * 🚀 BƯỚC 1: Freelancer bắt đầu làm milestone
     * 
     * Status: PENDING → IN_PROGRESS
     * 
     * Validate:
     * - Freelancer phải được assign vào project
     * - Milestone phải đang PENDING
     * - ⚠️ KHÔNG được bắt đầu milestone tiếp theo nếu milestone trước chưa APPROVED
     * 
     * @param freelancerId ID của freelancer
     * @param milestoneId ID của milestone
     * @return Milestone đã cập nhật
     */
    @Transactional
    public Milestone startMilestone(UUID freelancerId, UUID milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
            .orElseThrow(() -> new RuntimeException("Milestone not found"));
        
        // Validate freelancer owns the project
        Project project = projectRepository.findById(milestone.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getFreelancerId().equals(freelancerId)) {
            throw new RuntimeException("You are not assigned to this project");
        }
        
        // Validate milestone status
        if (milestone.getStatus() != Milestone.MilestoneStatus.PENDING) {
            throw new RuntimeException("Can only start PENDING milestones. Current status: " + milestone.getStatus());
        }
        
        // ⚠️ VALIDATE TUẦN TỰ: Kiểm tra milestone trước đã APPROVED chưa
        validateMilestoneSequence(milestone);
        
        // Update status
        milestone.setStatus(Milestone.MilestoneStatus.IN_PROGRESS);
        Milestone saved = milestoneRepository.save(milestone);
        
        log.info("✅ MILESTONE STARTED: {} (seq: {}) by freelancer: {}", 
            milestoneId, milestone.getSequenceOrder(), freelancerId);
        
        return saved;
    }
    
    /**
     * 📤 BƯỚC 2: Freelancer submit milestone để Employer review
     * 
     * Status: IN_PROGRESS → SUBMITTED
     * 
     * Freelancer cần gửi:
     * - Deliverables (mô tả hoặc link file đã hoàn thành)
     * - Completion notes (ghi chú về công việc)
     * 
     * @param freelancerId ID của freelancer
     * @param milestoneId ID của milestone
     * @param deliverables Mô tả kết quả hoặc link file
     * @param notes Ghi chú bổ sung
     * @return Milestone đã submit
     */
    @Transactional
    public Milestone submitMilestoneForReview(UUID freelancerId, UUID milestoneId, 
                                              String deliverables, String notes) {
        
        Milestone milestone = milestoneRepository.findById(milestoneId)
            .orElseThrow(() -> new RuntimeException("Milestone not found"));
        
        Project project = projectRepository.findById(milestone.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getFreelancerId().equals(freelancerId)) {
            throw new RuntimeException("Not authorized");
        }
        
        // Validate status
        if (milestone.getStatus() != Milestone.MilestoneStatus.IN_PROGRESS) {
            throw new RuntimeException("Can only submit IN_PROGRESS milestones. Current status: " + milestone.getStatus());
        }
        
        // Validate deliverables không được rỗng
        if (deliverables == null || deliverables.trim().isEmpty()) {
            throw new RuntimeException("Deliverables cannot be empty");
        }
        
        // Update status and store deliverables
        milestone.setStatus(Milestone.MilestoneStatus.SUBMITTED);
        milestone.setSubmittedAt(LocalDateTime.now());
        milestone.setDeliverables(deliverables);
        milestone.setCompletionNotes(notes);
        
        Milestone saved = milestoneRepository.save(milestone);
        log.info("✅ MILESTONE SUBMITTED: {} (seq: {}) - Waiting employer review", 
            milestoneId, milestone.getSequenceOrder());
        
        return saved;
    }
    
    /**
     * ✅ BƯỚC 3: Employer approve milestone & giải ngân tiền
     * 
     * Status: SUBMITTED → APPROVED → RELEASED
     * 
     * 🚨 ĐÂY LÀ BƯỚC QUAN TRỌNG - GIẢI NGÂN TIỀN!
     * 
     * Khi Employer approve:
     * 1. Giải ngân tiền từ Escrow cho Freelancer
     * 2. Cập nhật milestone status → APPROVED
     * 3. Kiểm tra xem tất cả milestones đã xong chưa
     * 4. Nếu xong hết → Complete project
     * 
     * @param employerId ID của employer
     * @param milestoneId ID của milestone
     * @return Milestone đã approve
     */
    @Transactional
    public Milestone approveMilestone(UUID employerId, UUID milestoneId) {
        
        Milestone milestone = milestoneRepository.findById(milestoneId)
            .orElseThrow(() -> new RuntimeException("Milestone not found"));
        
        Project project = projectRepository.findById(milestone.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getEmployerId().equals(employerId)) {
            throw new RuntimeException("Not authorized");
        }
        
        // Validate status
        if (milestone.getStatus() != Milestone.MilestoneStatus.SUBMITTED) {
            throw new RuntimeException("Can only approve SUBMITTED milestones. Current status: " + milestone.getStatus());
        }
        
        log.info("💸 Approving milestone {} - Releasing {} USDT to freelancer", 
            milestoneId, milestone.getAmount());
        
        // Giải ngân tiền từ escrow
        boolean released = pointsService.releaseEscrowToFreelancer(
            employerId,
            project.getFreelancerId(),
            milestone.getAmount(),
            project.getId().toString()
        );
        
        if (!released) {
            throw new RuntimeException("Failed to release payment");
        }
        
        // Cập nhật status
        milestone.setStatus(Milestone.MilestoneStatus.APPROVED);
        milestone.setApprovedAt(LocalDateTime.now());
        
        Milestone saved = milestoneRepository.save(milestone);
        log.info("✅ MILESTONE APPROVED: {} (seq: {}) - {} USDT released to freelancer", 
            milestoneId, milestone.getSequenceOrder(), milestone.getAmount());
        
        // Update freelancer profile stats: Add to total earnings
        freelancerProfileService.addToTotalEarnings(project.getFreelancerId(), milestone.getAmount());
        log.debug("💰 Added {} to freelancer {} total_earnings", milestone.getAmount(), project.getFreelancerId());
        
        // Kiểm tra xem tất cả milestones đã hoàn thành chưa
        checkAndCompleteProject(project.getId());
        
        return saved;
    }
    
    /**
     * ❌ Employer reject milestone - yêu cầu làm lại
     * 
     * Status: SUBMITTED → IN_PROGRESS (quay lại)
     * 
     * Nếu Employer thấy kết quả chưa đạt yêu cầu:
     * - Gửi feedback/rejection reason
     * - Milestone quay lại IN_PROGRESS
     * - Freelancer phải sửa và submit lại
     * 
     * @param employerId ID của employer
     * @param milestoneId ID của milestone
     * @param reason Lý do reject & yêu cầu sửa
     * @return Milestone đã reject
     */
    @Transactional
    public Milestone rejectMilestone(UUID employerId, UUID milestoneId, String reason) {
        
        Milestone milestone = milestoneRepository.findById(milestoneId)
            .orElseThrow(() -> new RuntimeException("Milestone not found"));
        
        Project project = projectRepository.findById(milestone.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getEmployerId().equals(employerId)) {
            throw new RuntimeException("Not authorized");
        }
        
        // Validate status
        if (milestone.getStatus() != Milestone.MilestoneStatus.SUBMITTED) {
            throw new RuntimeException("Can only reject SUBMITTED milestones. Current status: " + milestone.getStatus());
        }
        
        // Validate reason không được rỗng
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("Rejection reason is required");
        }
        
        // Quay lại IN_PROGRESS để freelancer sửa
        milestone.setStatus(Milestone.MilestoneStatus.IN_PROGRESS);
        milestone.setRejectionReason(reason);
        milestone.setSubmittedAt(null); // Clear submission timestamp
        
        Milestone saved = milestoneRepository.save(milestone);
        log.info("❌ MILESTONE REJECTED: {} (seq: {}) - Reason: {}", 
            milestoneId, milestone.getSequenceOrder(), reason);
        
        return saved;
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    /**
     * ⚠️ VALIDATE TUẦN TỰ MILESTONE - KHÔNG ĐƯỢC SKIP!
     * 
     * Quy tắc: Phải hoàn thành milestone trước khi bắt đầu milestone tiếp theo
     * 
     * Ví dụ:
     * - Milestone 1 (seq=1): APPROVED ✅
     * - Milestone 2 (seq=2): IN_PROGRESS ✅ OK - có thể làm
     * - Milestone 3 (seq=3): PENDING ❌ KHÔNG được bắt đầu vì milestone 2 chưa xong
     * 
     * @param currentMilestone Milestone hiện tại muốn bắt đầu
     * @throws RuntimeException nếu milestone trước chưa hoàn thành
     */
    private void validateMilestoneSequence(Milestone currentMilestone) {
        // Nếu là milestone đầu tiên (seq=1) thì luôn OK
        if (currentMilestone.getSequenceOrder() == 1) {
            return;
        }
        
        // Lấy tất cả milestones của project, sắp xếp theo sequence
        List<Milestone> allMilestones = milestoneRepository
            .findByProjectIdOrderBySequenceOrder(currentMilestone.getProjectId());
        
        // Kiểm tra tất cả milestones trước đó phải APPROVED
        for (Milestone m : allMilestones) {
            if (m.getSequenceOrder() < currentMilestone.getSequenceOrder()) {
                if (m.getStatus() != Milestone.MilestoneStatus.APPROVED && 
                    m.getStatus() != Milestone.MilestoneStatus.RELEASED) {
                    throw new RuntimeException(
                        "⚠️ Cannot start milestone " + currentMilestone.getSequenceOrder() + 
                        " - Previous milestone (" + m.getSequenceOrder() + ") must be completed first! " +
                        "Current status: " + m.getStatus()
                    );
                }
            }
        }
        
        log.info("✅ Milestone sequence validated - All previous milestones completed");
    }
    
    /**
     * Kiểm tra xem tất cả milestones đã hoàn thành chưa
     * Nếu rồi → đánh dấu project là COMPLETED
     */
    private void checkAndCompleteProject(UUID projectId) {
        
        List<Milestone> milestones = milestoneRepository.findByProjectIdOrderBySequenceOrder(projectId);
        
        boolean allCompleted = milestones.stream()
            .allMatch(m -> m.getStatus() == Milestone.MilestoneStatus.APPROVED 
                        || m.getStatus() == Milestone.MilestoneStatus.RELEASED);
        
        if (allCompleted) {
            Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
            
            project.setStatus(Project.ProjectStatus.COMPLETED);
            project.setCompletedAt(LocalDateTime.now());
            projectRepository.save(project);
            
            // Update employer stats: Decrement active_projects, Add to total_spent
            employerProfileService.decrementActiveProjects(project.getEmployerId());
            employerProfileService.addToTotalSpent(project.getEmployerId(), project.getAgreedAmount());
            log.debug("📉 Decremented active_projects and added {} to total_spent for employer: {}", 
                project.getAgreedAmount(), project.getEmployerId());
            
            // Update freelancer stats: Increment jobs_completed
            freelancerProfileService.incrementJobsCompleted(project.getFreelancerId());
            log.debug("📈 Incremented jobs_completed for freelancer: {}", project.getFreelancerId());
            
            log.info("✅ Project completed: {}", projectId);
        }
    }

    /**
     * 🆕 CREATE DEFAULT MILESTONE (on project creation)
     */
    @Transactional
    public Milestone createDefaultMilestone(UUID projectId, BigDecimal amount, int estimatedDurationDays) {
        
        // 1. Validate project exists
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // 2. Create default milestone
        Milestone milestone = Milestone.builder()
            .projectId(projectId)
            .title("Project Completion")
            .description("Full payment upon project completion")
            .amount(amount)
            .currency("USDT")
            .sequenceOrder(1)
            .status(Milestone.MilestoneStatus.IN_PROGRESS) // 🆕 CHANGE: Set to IN_PROGRESS instead of PENDING
            .dueDate(LocalDateTime.now().plusDays(estimatedDurationDays))
            .build();
        
        milestoneRepository.save(milestone);
        log.info("✅ Default milestone created (IN_PROGRESS) for project: {}", projectId);
        
        return milestone;
    }
}