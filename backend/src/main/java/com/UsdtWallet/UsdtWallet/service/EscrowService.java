package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.dto.response.EscrowResponse;
import com.UsdtWallet.UsdtWallet.model.entity.*;
import com.UsdtWallet.UsdtWallet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quản lý Escrow (Ký quỹ)
 * 
 * Luồng nghiệp vụ Escrow:
 * 1. LOCK: Khi tạo milestone, employer lock funds vào escrow
 * 2. RELEASED: Khi milestone approved, funds được release cho freelancer
 * 3. REFUNDED: Khi dispute resolved hoặc milestone rejected, funds được refund về employer
 * 4. DISPUTED: Khi có tranh chấp, escrow bị giữ lại chờ admin xử lý
 * 
 * Platform Fee: Hệ thống tự động tính và trừ phí (ví dụ: 10%)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EscrowService {

    private final EscrowRepository escrowRepository;
    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PointsService pointsService;
    private final NotificationService notificationService;

    // Platform fee: 10%
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.10");

    /**
     * Lock funds vào escrow khi tạo milestone
     * 
     * @param milestoneId ID của milestone
     * @param employerId ID của employer
     * @return Escrow record vừa tạo
     */
    @Transactional
    public EscrowResponse lockFundsForMilestone(UUID milestoneId, UUID employerId) {
        // Validate milestone exists
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        // Validate project
        Project project = projectRepository.findById(milestone.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getEmployerId().equals(employerId)) {
            throw new RuntimeException("Only employer can lock funds");
        }

        // Check if escrow already exists
        if (escrowRepository.existsByMilestoneId(milestoneId)) {
            throw new RuntimeException("Escrow already exists for this milestone");
        }

        // Calculate platform fee
        BigDecimal amount = milestone.getAmount();
        BigDecimal platformFee = amount.multiply(PLATFORM_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        // Check employer balance
        BigDecimal requiredAmount = amount; // Total = milestone amount
        BigDecimal availableBalance = pointsService.getAvailableBalance(employerId);
        
        if (availableBalance.compareTo(requiredAmount) < 0) {
            throw new RuntimeException(
                "Insufficient balance. Required: " + requiredAmount + 
                " PTS, Available: " + availableBalance + " PTS"
            );
        }

        // Lock funds (debit from employer)
        pointsService.lockFundsToEscrow(
            employerId, 
            project.getId(), 
            milestoneId, 
            amount
        );

        // Create escrow record
        Escrow escrow = Escrow.builder()
                .projectId(project.getId())
                .milestoneId(milestoneId)
                .employerId(employerId)
                .freelancerId(project.getFreelancerId())
                .amount(amount)
                .platformFee(platformFee)
                .currency("USDT")
                .status(Escrow.EscrowStatus.LOCKED)
                .lockedAt(LocalDateTime.now())
                .build();

        escrow = escrowRepository.save(escrow);
        log.info("✅ Escrow locked: {} for milestone: {}, amount: {}", 
                escrow.getId(), milestoneId, amount);

        // Notify freelancer
        notificationService.createNotification(
                project.getFreelancerId(),
                Notification.NotificationType.MILESTONE_CREATED,
                "Funds Locked in Escrow",
                "Employer has locked " + amount + " PTS for milestone: " + milestone.getTitle(),
                "MILESTONE",
                milestoneId
        );

        return mapToResponse(escrow);
    }

    /**
     * Release funds từ escrow khi milestone approved
     * 
     * @param milestoneId ID của milestone
     * @param employerId ID của employer (người approve)
     * @return Escrow record đã update
     */
    @Transactional
    public EscrowResponse releaseFunds(UUID milestoneId, UUID employerId) {
        // Get escrow
        Escrow escrow = escrowRepository.findByMilestoneId(milestoneId)
                .orElseThrow(() -> new RuntimeException("Escrow not found for this milestone"));

        if (escrow.getStatus() != Escrow.EscrowStatus.LOCKED) {
            throw new RuntimeException("Escrow is not in LOCKED state");
        }

        // Validate employer
        if (!escrow.getEmployerId().equals(employerId)) {
            throw new RuntimeException("Only employer can release funds");
        }

        // Calculate amounts
        BigDecimal totalAmount = escrow.getAmount();
        BigDecimal platformFee = escrow.getPlatformFee();
        BigDecimal freelancerAmount = totalAmount.subtract(platformFee);

        // Release funds to freelancer
        pointsService.releaseEscrowToFreelancer(
                escrow.getFreelancerId(),
                escrow.getProjectId(),
                milestoneId,
                freelancerAmount
        );

        // Update escrow status
        escrow.setStatus(Escrow.EscrowStatus.RELEASED);
        escrow.setReleasedAt(LocalDateTime.now());
        escrow.setReleasedTo(escrow.getFreelancerId());
        escrow = escrowRepository.save(escrow);

        log.info("✅ Escrow released: {}, freelancer gets: {}, platform fee: {}", 
                escrow.getId(), freelancerAmount, platformFee);

        // Notify freelancer
        notificationService.createNotification(
                escrow.getFreelancerId(),
                Notification.NotificationType.PAYMENT_RECEIVED,
                "Payment Received",
                "You received " + freelancerAmount + " PTS for completed milestone",
                "MILESTONE",
                milestoneId
        );

        return mapToResponse(escrow);
    }

    /**
     * Refund funds về employer khi milestone rejected hoặc dispute
     * 
     * @param milestoneId ID của milestone
     * @param adminId ID của admin (hoặc employer)
     * @return Escrow record đã update
     */
    @Transactional
    public EscrowResponse refundFunds(UUID milestoneId, UUID adminId) {
        // Get escrow
        Escrow escrow = escrowRepository.findByMilestoneId(milestoneId)
                .orElseThrow(() -> new RuntimeException("Escrow not found for this milestone"));

        if (escrow.getStatus() != Escrow.EscrowStatus.LOCKED && 
            escrow.getStatus() != Escrow.EscrowStatus.DISPUTED) {
            throw new RuntimeException("Cannot refund escrow in current state: " + escrow.getStatus());
        }

        // Refund full amount to employer
        pointsService.refundEscrowToEmployer(
                escrow.getEmployerId(),
                escrow.getProjectId(),
                milestoneId,
                escrow.getAmount()
        );

        // Update escrow status
        escrow.setStatus(Escrow.EscrowStatus.REFUNDED);
        escrow.setRefundedAt(LocalDateTime.now());
        escrow = escrowRepository.save(escrow);

        log.info("✅ Escrow refunded: {}, amount: {} returned to employer", 
                escrow.getId(), escrow.getAmount());

        // Notify employer
        notificationService.createNotification(
                escrow.getEmployerId(),
                Notification.NotificationType.PAYMENT_RECEIVED,
                "Escrow Refunded",
                escrow.getAmount() + " PTS has been refunded to your account",
                "MILESTONE",
                milestoneId
        );

        // Notify freelancer
        notificationService.createNotification(
                escrow.getFreelancerId(),
                Notification.NotificationType.SYSTEM_ALERT,
                "Escrow Refunded",
                "Milestone escrow has been refunded to employer",
                "MILESTONE",
                milestoneId
        );

        return mapToResponse(escrow);
    }

    /**
     * Mark escrow as disputed
     */
    @Transactional
    public EscrowResponse markAsDisputed(UUID milestoneId) {
        Escrow escrow = escrowRepository.findByMilestoneId(milestoneId)
                .orElseThrow(() -> new RuntimeException("Escrow not found for this milestone"));

        if (escrow.getStatus() != Escrow.EscrowStatus.LOCKED) {
            throw new RuntimeException("Only LOCKED escrow can be disputed");
        }

        escrow.setStatus(Escrow.EscrowStatus.DISPUTED);
        escrow = escrowRepository.save(escrow);

        log.info("⚠️ Escrow marked as disputed: {}", escrow.getId());

        return mapToResponse(escrow);
    }

    /**
     * Lấy escrow theo milestone ID
     */
    public EscrowResponse getEscrowByMilestoneId(UUID milestoneId) {
        Escrow escrow = escrowRepository.findByMilestoneId(milestoneId)
                .orElseThrow(() -> new RuntimeException("Escrow not found for this milestone"));
        return mapToResponse(escrow);
    }

    /**
     * Lấy tất cả escrow của một project
     */
    public List<EscrowResponse> getEscrowsByProject(UUID projectId) {
        List<Escrow> escrows = escrowRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return escrows.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tổng số tiền đang lock trong escrow của project
     */
    public BigDecimal getTotalLockedAmount(UUID projectId) {
        return escrowRepository.findTotalLockedAmountByProject(projectId);
    }

    /**
     * Lấy escrow statistics cho employer
     */
    public EscrowStatistics getEmployerEscrowStatistics(UUID employerId) {
        List<Escrow> allEscrows = escrowRepository.findByEmployerIdOrderByCreatedAtDesc(employerId);
        
        BigDecimal totalLocked = allEscrows.stream()
                .filter(e -> e.getStatus() == Escrow.EscrowStatus.LOCKED)
                .map(Escrow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReleased = allEscrows.stream()
                .filter(e -> e.getStatus() == Escrow.EscrowStatus.RELEASED)
                .map(Escrow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRefunded = allEscrows.stream()
                .filter(e -> e.getStatus() == Escrow.EscrowStatus.REFUNDED)
                .map(Escrow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalDisputed = allEscrows.stream()
                .filter(e -> e.getStatus() == Escrow.EscrowStatus.DISPUTED)
                .count();

        return EscrowStatistics.builder()
                .userId(employerId)
                .totalLocked(totalLocked)
                .totalReleased(totalReleased)
                .totalRefunded(totalRefunded)
                .totalDisputed(totalDisputed)
                .build();
    }

    /**
     * Map entity to response
     */
    private EscrowResponse mapToResponse(Escrow escrow) {
        Milestone milestone = escrow.getMilestoneId() != null ?
                milestoneRepository.findById(escrow.getMilestoneId()).orElse(null) : null;

        return EscrowResponse.builder()
                .id(escrow.getId())
                .projectId(escrow.getProjectId())
                .milestoneId(escrow.getMilestoneId())
                .milestoneTitle(milestone != null ? milestone.getTitle() : null)
                .employerId(escrow.getEmployerId())
                .freelancerId(escrow.getFreelancerId())
                .amount(escrow.getAmount())
                .platformFee(escrow.getPlatformFee())
                .currency(escrow.getCurrency())
                .status(escrow.getStatus().name())
                .lockedAt(escrow.getLockedAt())
                .releasedAt(escrow.getReleasedAt())
                .refundedAt(escrow.getRefundedAt())
                .releasedTo(escrow.getReleasedTo())
                .createdAt(escrow.getCreatedAt())
                .updatedAt(escrow.getUpdatedAt())
                .build();
    }

    /**
     * Inner class for escrow statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class EscrowStatistics {
        private UUID userId;
        private BigDecimal totalLocked;
        private BigDecimal totalReleased;
        private BigDecimal totalRefunded;
        private long totalDisputed;
    }
}
