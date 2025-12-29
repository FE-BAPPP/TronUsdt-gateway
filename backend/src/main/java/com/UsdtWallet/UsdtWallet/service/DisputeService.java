package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.dto.request.DisputeCreateRequest;
import com.UsdtWallet.UsdtWallet.model.dto.request.DisputeResolveRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.DisputeResponse;
import com.UsdtWallet.UsdtWallet.model.entity.Dispute;
import com.UsdtWallet.UsdtWallet.model.entity.Project;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import com.UsdtWallet.UsdtWallet.repository.DisputeRepository;
import com.UsdtWallet.UsdtWallet.repository.ProjectRepository;
import com.UsdtWallet.UsdtWallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeService {
    
    private final DisputeRepository disputeRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PointsService pointsService;
    private final NotificationService notificationService;
    
    /**
     * Create dispute for a project
     */
    @Transactional
    public DisputeResponse createDispute(UUID userId, DisputeCreateRequest request) {
        // Validate project exists
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Validate user is part of the project
        if (!project.getEmployerId().equals(userId) && !project.getFreelancerId().equals(userId)) {
            throw new RuntimeException("You are not authorized to raise a dispute for this project");
        }
        
        // Check if dispute already exists
        if (disputeRepository.existsByProjectIdAndStatusNot(
                request.getProjectId(), Dispute.DisputeStatus.CLOSED)) {
            throw new RuntimeException("An active dispute already exists for this project");
        }
        
        // Create dispute
        Dispute dispute = Dispute.builder()
                .projectId(request.getProjectId())
                .raisedBy(userId)
                .reason(request.getReason())
                .evidence(request.getEvidence())
                .status(Dispute.DisputeStatus.OPEN)
                .build();
        
        dispute = disputeRepository.save(dispute);
        
        // Update project status
        project.setStatus(Project.ProjectStatus.DISPUTED);
        projectRepository.save(project);
        
        // Notify other party
        UUID otherPartyId = project.getEmployerId().equals(userId) 
                ? project.getFreelancerId() 
                : project.getEmployerId();
        notificationService.createNotification(
                otherPartyId,
                com.UsdtWallet.UsdtWallet.model.entity.Notification.NotificationType.DISPUTE_OPENED,
                "Dispute Opened",
                "A dispute has been raised for your project. An admin will review it shortly.",
                "DISPUTE",
                dispute.getId()
        );
        
        log.info("Dispute created: {} for project: {} by user: {}", 
                dispute.getId(), request.getProjectId(), userId);
        
        return mapToResponse(dispute);
    }
    
    /**
     * Get disputes (Admin view)
     */
    public Page<DisputeResponse> getAllDisputes(Dispute.DisputeStatus status, Pageable pageable) {
        Page<Dispute> disputes = status != null 
                ? disputeRepository.findByStatus(status, pageable)
                : disputeRepository.findAll(pageable);
        
        return disputes.map(this::mapToResponse);
    }
    
    /**
     * Get user's disputes
     */
    public Page<DisputeResponse> getUserDisputes(UUID userId, Pageable pageable) {
        Page<Dispute> disputes = disputeRepository.findByRaisedBy(userId, pageable);
        return disputes.map(this::mapToResponse);
    }
    
    /**
     * Get dispute details
     */
    public DisputeResponse getDisputeDetails(UUID disputeId, UUID userId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));
        
        // Validate access
        Project project = projectRepository.findById(dispute.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        boolean isPartyInvolved = project.getEmployerId().equals(userId) 
                || project.getFreelancerId().equals(userId);
        
        if (!isAdmin && !isPartyInvolved) {
            throw new RuntimeException("You are not authorized to view this dispute");
        }
        
        return mapToResponse(dispute);
    }
    
    /**
     * Resolve dispute (Admin only)
     */
    @Transactional
    public DisputeResponse resolveDispute(UUID disputeId, UUID adminId, DisputeResolveRequest request) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));
        
        if (dispute.getStatus() == Dispute.DisputeStatus.RESOLVED || 
            dispute.getStatus() == Dispute.DisputeStatus.CLOSED) {
            throw new RuntimeException("Dispute is already resolved or closed");
        }
        
        Project project = projectRepository.findById(dispute.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Update dispute
        dispute.setStatus(Dispute.DisputeStatus.RESOLVED);
        dispute.setResolvedBy(adminId);
        dispute.setResolution(request.getResolution());
        dispute.setAdminNotes(request.getAdminNotes());
        dispute.setRefundAmount(request.getRefundAmount());
        dispute.setResolvedAt(LocalDateTime.now());
        
        dispute = disputeRepository.save(dispute);
        
        // Process refund if specified
        if (request.getRefundAmount() != null && request.getRefundAmount().signum() > 0) {
            pointsService.refundEscrow(
                    project.getEmployerId(),
                    project.getFreelancerId(),
                    request.getRefundAmount(),
                    project.getId().toString()
            );
        }
        
        // Update project status
        project.setStatus(Project.ProjectStatus.CANCELLED);
        projectRepository.save(project);
        
        // Notify both parties
        notifyDisputeResolved(dispute, project);
        
        log.info("Dispute resolved: {} by admin: {}", disputeId, adminId);
        
        return mapToResponse(dispute);
    }
    
    /**
     * Close dispute
     */
    @Transactional
    public void closeDispute(UUID disputeId, UUID adminId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));
        
        dispute.setStatus(Dispute.DisputeStatus.CLOSED);
        disputeRepository.save(dispute);
        
        log.info("Dispute closed: {} by admin: {}", disputeId, adminId);
    }
    
    /**
     * Notify parties about dispute resolution
     */
    private void notifyDisputeResolved(Dispute dispute, Project project) {
        String message = "The dispute for your project has been resolved. Please check the details.";
        
        notificationService.createNotification(
                project.getEmployerId(),
                com.UsdtWallet.UsdtWallet.model.entity.Notification.NotificationType.DISPUTE_RESOLVED,
                "Dispute Resolved",
                message,
                "DISPUTE",
                dispute.getId()
        );
        
        notificationService.createNotification(
                project.getFreelancerId(),
                com.UsdtWallet.UsdtWallet.model.entity.Notification.NotificationType.DISPUTE_RESOLVED,
                "Dispute Resolved",
                message,
                "DISPUTE",
                dispute.getId()
        );
    }
    
    /**
     * Map Dispute entity to response DTO
     */
    private DisputeResponse mapToResponse(Dispute dispute) {
        Project project = projectRepository.findById(dispute.getProjectId()).orElse(null);
        User raisedByUser = userRepository.findById(dispute.getRaisedBy()).orElse(null);
        User resolvedByUser = dispute.getResolvedBy() != null 
                ? userRepository.findById(dispute.getResolvedBy()).orElse(null) 
                : null;
        
        return DisputeResponse.builder()
                .id(dispute.getId())
                .projectId(dispute.getProjectId())
                .projectTitle(project != null ? project.getJobId().toString() : null)
                .raisedBy(dispute.getRaisedBy())
                .raisedByName(raisedByUser != null ? raisedByUser.getFullName() : "Unknown")
                .reason(dispute.getReason())
                .evidence(dispute.getEvidence())
                .status(dispute.getStatus().name())
                .adminNotes(dispute.getAdminNotes())
                .resolvedBy(dispute.getResolvedBy())
                .resolvedByName(resolvedByUser != null ? resolvedByUser.getFullName() : null)
                .resolution(dispute.getResolution())
                .refundAmount(dispute.getRefundAmount())
                .resolvedAt(dispute.getResolvedAt())
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .build();
    }
}