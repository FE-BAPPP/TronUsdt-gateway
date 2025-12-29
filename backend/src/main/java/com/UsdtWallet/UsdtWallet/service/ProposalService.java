package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.dto.request.ProposalCreateRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.ProposalResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final ProjectRepository projectRepository;
    private final EscrowRepository escrowRepository;
    private final MilestoneRepository milestoneRepository; // 🆕 ADD
    private final ConversationService conversationService; // 🆕 ADD
    private final NotificationService notificationService;
    private final PointsService pointsService; // Add PointsService
    private final EmployerProfileService employerProfileService; // ✅ ADD
    private final FreelancerProfileService freelancerProfileService; // ✅ ADD (need to create)

    // ========================================================================
    // LUỒNG FREELANCER: TìM JOB & GỬI PROPOSAL
    // ========================================================================

    /**
     * 📝 LUỒNG: Freelancer gửi proposal cho một job
     * 
     * BƯớc trong luồng nghiệp vụ:
     * 1. Freelancer duyệt danh sách job
     * 2. Chọn job phù hợp và đọc yêu cầu
     * 3. Gửi proposal với giá đề xuất và timeline
     * 4. Chờ Employer review và chấp nhận
     * 
     * Validate:
     * - Job phải đang OPEN
     * - Proposal amount <= job budget
     * - Chưa gửi proposal cho job này trước đó
     * - User phải có role FREELANCER
     * 
     * @param freelancerId ID của freelancer
     * @param request Thông tin proposal (cover letter, giá, timeline)
     * @return Proposal vừa tạo
     */
    @Transactional
    public ProposalResponse submitProposal(UUID freelancerId, ProposalCreateRequest request) {
        // BƯỚC 1: Validate job exists and is OPEN
        Job job = jobRepository.findById(request.getJobId())
            .orElseThrow(() -> new RuntimeException("Job not found"));

        if (job.getStatus() != Job.JobStatus.OPEN) {
            throw new RuntimeException("Job is no longer open for proposals");
        }

        // BƯỚC 2: Validate proposal amount <= job budget
        if (request.getProposedAmount().compareTo(job.getBudget()) > 0) {
            throw new RuntimeException(
                "Proposal amount cannot exceed job budget. " +
                "Job budget: " + job.getBudget() + " " + job.getCurrency()
            );
        }

        // BƯỚC 3: Check if freelancer already submitted proposal for this job
        boolean alreadySubmitted = proposalRepository.existsByJobIdAndFreelancerId(
            request.getJobId(), freelancerId);
        
        if (alreadySubmitted) {
            throw new RuntimeException("You have already submitted a proposal for this job");
        }

        // BƯỚC 4: Validate freelancer exists và có role FREELANCER
        User freelancer = userRepository.findById(freelancerId)
            .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        if (freelancer.getRole() != User.Role.FREELANCER) {
            throw new RuntimeException("Only freelancers can submit proposals");
        }

        // BƯỚC 5: Create proposal (status = PENDING)
        Proposal proposal = Proposal.builder()
            .jobId(request.getJobId())
            .freelancerId(freelancerId)
            .coverLetter(request.getCoverLetter())
            .proposedAmount(request.getProposedAmount())
            .estimatedDurationDays(request.getEstimatedDurationDays())
            .status(Proposal.ProposalStatus.PENDING)
            .build();

        Proposal savedProposal = proposalRepository.save(proposal);
        log.info("✅ PROPOSAL SUBMITTED: {} for job: {} by freelancer: {}", 
            savedProposal.getId(), job.getId(), freelancerId);


        return mapToProposalResponse(savedProposal, job, freelancer);
    }

    // ========================================================================
    // LUỒNG EMPLOYER: XEM & CHỌ PROPOSAL
    // ========================================================================

    /**
     * 👀 Lấy danh sách proposals cho 1 job (Employer view)
     * 
     * Employer sẽ xem tất cả proposals đã nhận để:
     * - So sánh giá, timeline
     * - Đọc cover letter
     * - Xem profile freelancer
     * - Chọn freelancer phù hợp nhất
     * 
     * @param jobId ID của job
     * @param employerId ID của employer (để validate authorization)
     * @param pageable Pagination
     * @return Danh sách proposals
     */
    public Page<ProposalResponse> getProposalsForJob(UUID jobId, UUID employerId, Pageable pageable) {
        // Validate employer owns this job
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getEmployerId().equals(employerId)) {
            throw new RuntimeException("You are not authorized to view proposals for this job");
        }

        return proposalRepository.findByJobIdOrderByCreatedAtDesc(jobId, pageable)
            .map(proposal -> {
                User freelancer = userRepository.findById(proposal.getFreelancerId()).orElse(null);
                return mapToProposalResponse(proposal, job, freelancer);
            });
    }

    /**
     * Get freelancer's submitted proposals
     */
    public Page<ProposalResponse> getFreelancerProposals(UUID freelancerId, Pageable pageable) {
        return proposalRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancerId, pageable)
            .map(proposal -> {
                Job job = jobRepository.findById(proposal.getJobId()).orElse(null);
                User freelancer = userRepository.findById(freelancerId).orElse(null);
                return mapToProposalResponse(proposal, job, freelancer);
            });
    }

    /**
     * Get single proposal details
     */
    public ProposalResponse getProposalById(UUID proposalId, UUID userId) {
        Proposal proposal = proposalRepository.findById(proposalId)
            .orElseThrow(() -> new RuntimeException("Proposal not found"));

        Job job = jobRepository.findById(proposal.getJobId()).orElse(null);
        
        // Check authorization: either employer or freelancer
        if (job != null && !job.getEmployerId().equals(userId) && 
            !proposal.getFreelancerId().equals(userId)) {
            throw new RuntimeException("Not authorized to view this proposal");
        }

        User freelancer = userRepository.findById(proposal.getFreelancerId()).orElse(null);
        return mapToProposalResponse(proposal, job, freelancer);
    }

    /**
     * ✅ LUỒNG CORE: Employer chấp nhận proposal & tạo project
     * 
     * 🚨 ĐÂY LÀ LUỒNG QUAN TRỌNG NHẤT - BẮT ĐẦU DỰ ÁN!
     * 
     * Khi Employer chọn "Hire" cho 1 proposal:
     * 
     * BƯỚC 1: Validate proposal và job
     *   - Proposal phải PENDING
     *   - Employer phải sở hữu job
     *   - Employer phải có đủ số dư Points
     * 
     * BƯỚC 2: Tạo PROJECT (status = IN_PROGRESS)
     *   - Liên kết job + employer + freelancer
     *   - Lưu agreed amount
     * 
     * BƯỚC 3: Tạo ESCROW & LOCK FUNDS
     *   - Lock points của employer vào escrow
     *   - Status = LOCKED
     *   - Số tiền này sẽ được giải ngân theo milestones
     * 
     * BƯỚC 4: Tạo DEFAULT MILESTONE
     *   - 1 milestone full payment
     *   - Có thể chia thêm sau
     * 
     * BƯỚC 5: Tạo CONVERSATION cho project
     *   - Employer và Freelancer có thể chat
     * 
     * BƯỚC 6: Cập nhật trạng thái
     *   - Proposal: PENDING → AWARDED
     *   - Job: OPEN → IN_PROGRESS
     *   - Các proposals khác: PENDING → REJECTED
     * 
     * @param proposalId ID proposal được chọn
     * @param employerId ID employer thực hiện
     * @return Proposal response
     */
    @Transactional
    public ProposalResponse awardProposal(UUID proposalId, UUID employerId) {
        // ===== BƯỚC 1: VALIDATE =====
        Proposal proposal = proposalRepository.findById(proposalId)
            .orElseThrow(() -> new RuntimeException("Proposal not found"));
        
        if (proposal.getStatus() != Proposal.ProposalStatus.PENDING) {
            throw new RuntimeException("Only pending proposals can be awarded");
        }
        
        Job job = jobRepository.findById(proposal.getJobId())
            .orElseThrow(() -> new RuntimeException("Job not found"));
        
        if (!job.getEmployerId().equals(employerId)) {
            throw new RuntimeException("You are not authorized to award this proposal");
        }
        
        // Kiểm tra số dư available của employer
        BigDecimal availableBalance = pointsService.getAvailableBalance(employerId);
        if (availableBalance.compareTo(proposal.getProposedAmount()) < 0) {
            throw new RuntimeException("Insufficient balance. Available: " + availableBalance + " Points, Required: " + proposal.getProposedAmount());
        }
        
        // ===== BƯỚC 2: TẠO PROJECT =====
        log.info("🛠️ Creating project for job: {} | Employer: {} | Freelancer: {}", 
            job.getId(), employerId, proposal.getFreelancerId());
            
        Project project = Project.builder()
            .jobId(job.getId())
            .employerId(employerId)
            .freelancerId(proposal.getFreelancerId())
            .awardedProposalId(proposal.getId())
            .agreedAmount(proposal.getProposedAmount())
            .currency("USDT")
            .status(Project.ProjectStatus.IN_PROGRESS)
            .startedAt(LocalDateTime.now())
            .build();
        
        Project savedProject = projectRepository.save(project);
        log.info("✅ PROJECT CREATED: {} | Amount: {} USDT", savedProject.getId(), proposal.getProposedAmount());
        
        // ===== BƯỚC 3: TẠO ESCROW & LOCK FUNDS =====
        log.info("🔒 Locking {} USDT in escrow for project: {}", proposal.getProposedAmount(), savedProject.getId());
        
        Escrow escrow = Escrow.builder()
            .projectId(savedProject.getId())
            .employerId(employerId)
            .freelancerId(proposal.getFreelancerId())
            .amount(proposal.getProposedAmount())
            .currency("USDT")
            .status(Escrow.EscrowStatus.LOCKED)
            .lockedAt(LocalDateTime.now())
            .build();
        
        escrowRepository.save(escrow);
        
        // Lock points trong ledger (PENDING transaction)
        boolean locked = pointsService.lockPointsForProject(
            employerId, 
            proposal.getProposedAmount(), 
            savedProject.getId().toString()
        );
        
        if (!locked) {
            throw new RuntimeException("Failed to lock funds in escrow");
        }
        
        log.info("✅ ESCROW LOCKED: {} USDT for project: {}", proposal.getProposedAmount(), savedProject.getId());
        
        // ===== BƯỚC 4: TẠO DEFAULT MILESTONE =====
        Milestone milestone = Milestone.builder()
            .projectId(savedProject.getId())
            .title("Project Completion")
            .description("Full payment upon project completion")
            .amount(proposal.getProposedAmount())
            .currency("USDT")
            .sequenceOrder(1)
            .status(Milestone.MilestoneStatus.PENDING)
            .dueDate(LocalDateTime.now().plusDays(proposal.getEstimatedDurationDays()))
            .build();
        
        milestoneRepository.save(milestone);
        log.info("✅ MILESTONE CREATED: Full payment milestone for project: {}", savedProject.getId());
        
        // ===== BƯỚC 5: TẠO CONVERSATION =====
        conversationService.createConversationForProject(savedProject.getId());
        log.info("✅ CONVERSATION CREATED: Chat enabled for project: {}", savedProject.getId());
        
        // ===== BƯỚC 6: CẬP NHẬT TRẠNG THÁI =====
        // Cập nhật proposal: PENDING → AWARDED
        proposal.setStatus(Proposal.ProposalStatus.AWARDED);
        proposal.setAcceptedAt(LocalDateTime.now());
        Proposal savedProposal = proposalRepository.save(proposal);
        
        // Cập nhật job: OPEN → IN_PROGRESS
        job.setStatus(Job.JobStatus.IN_PROGRESS);
        jobRepository.save(job);
        
        // Từ chối các proposals khác: PENDING → REJECTED
        proposalRepository.findByJobIdAndStatus(job.getId(), Proposal.ProposalStatus.PENDING)
            .forEach(p -> {
                p.setStatus(Proposal.ProposalStatus.REJECTED);
                proposalRepository.save(p);
            });
        
        // ===== BƯỚC 7: CẬP NHẬT PROFILE STATS =====
        // Employer: Tăng active_projects
        employerProfileService.incrementActiveProjects(employerId);
        log.debug("📈 Incremented active_projects for employer: {}", employerId);
        
        log.info("🎉 ==================================================" );
        log.info("🎉 PROJECT STARTED SUCCESSFULLY!");
        log.info("🎉 Project: {} | Amount: {} USDT", savedProject.getId(), proposal.getProposedAmount());
        log.info("🎉 Employer: {} | Freelancer: {}", employerId, proposal.getFreelancerId());
        log.info("🎉 Escrow locked, milestone created, chat enabled");
        log.info("🎉 ==================================================" );
        
        User freelancer = userRepository.findById(proposal.getFreelancerId()).orElse(null);
        return mapToProposalResponse(savedProposal, job, freelancer);
    }

    /**
     * Map Proposal entity to ProposalResponse DTO
     */
    private ProposalResponse mapToProposalResponse(Proposal proposal, Job job, User freelancer) {
        ProposalResponse.ProposalResponseBuilder builder = ProposalResponse.builder()
            .id(proposal.getId())
            .jobId(proposal.getJobId())
            .freelancerId(proposal.getFreelancerId())
            .coverLetter(proposal.getCoverLetter())
            .proposedAmount(proposal.getProposedAmount())
            .estimatedDurationDays(proposal.getEstimatedDurationDays())
            .status(proposal.getStatus().name())
            .acceptedAt(proposal.getAcceptedAt())
            .createdAt(proposal.getCreatedAt())
            .updatedAt(proposal.getUpdatedAt());

        if (job != null) {
            builder.jobTitle(job.getTitle());
        }

        if (freelancer != null) {
            builder.freelancerName(freelancer.getFullName());
            
            // Get freelancer stats
            FreelancerProfile profile = freelancerProfileRepository.findByUserId(freelancer.getId())
                .orElse(null);
            
        }

        return builder.build();
    }

    /**
     * 🔄 UPDATE PROPOSAL - Freelancer cập nhật proposal của mình
     * 
     * BƯỚC 1: Validate proposal tồn tại
     * BƯỚC 2: Validate freelancer là owner
     * BƯỚC 3: Validate proposal vẫn PENDING (chưa được xét)
     * BƯỚC 4: Update các field
     */
    @Transactional
    public ProposalResponse updateProposal(UUID proposalId, UUID freelancerId, 
                                          ProposalCreateRequest request) {
        log.info("🔄 Updating proposal: {} by freelancer: {}", proposalId, freelancerId);

        // BƯỚC 1: Validate proposal exists
        Proposal proposal = proposalRepository.findById(proposalId)
            .orElseThrow(() -> new RuntimeException("Proposal not found"));

        // BƯỚC 2: Validate ownership
        if (!proposal.getFreelancerId().equals(freelancerId)) {
            throw new RuntimeException("You are not authorized to update this proposal");
        }

        // BƯỚC 3: Validate status
        if (proposal.getStatus() != Proposal.ProposalStatus.PENDING) {
            throw new RuntimeException("Cannot update proposal that is not PENDING");
        }

        // BƯỚC 4: Update fields
        proposal.setCoverLetter(request.getCoverLetter());
        proposal.setProposedAmount(request.getProposedAmount());
        proposal.setEstimatedDurationDays(request.getEstimatedDurationDays());

        Proposal updatedProposal = proposalRepository.save(proposal);
        log.info("✅ Proposal updated successfully: {}", proposalId);

        Job job = jobRepository.findById(proposal.getJobId()).orElse(null);
        User freelancer = userRepository.findById(freelancerId).orElse(null);

        return mapToProposalResponse(updatedProposal, job, freelancer);
    }

    /**
     * ❌ WITHDRAW PROPOSAL - Freelancer rút proposal
     * 
     * BƯỚC 1: Validate proposal tồn tại
     * BƯỚC 2: Validate freelancer là owner
     * BƯỚC 3: Validate proposal vẫn PENDING
     * BƯỚC 4: Đổi status thành WITHDRAWN
     */
    @Transactional
    public ProposalResponse withdrawProposal(UUID proposalId, UUID freelancerId) {
        log.info("❌ Withdrawing proposal: {} by freelancer: {}", proposalId, freelancerId);

        // BƯỚC 1: Validate proposal exists
        Proposal proposal = proposalRepository.findById(proposalId)
            .orElseThrow(() -> new RuntimeException("Proposal not found"));

        // BƯỚC 2: Validate ownership
        if (!proposal.getFreelancerId().equals(freelancerId)) {
            throw new RuntimeException("You are not authorized to withdraw this proposal");
        }

        // BƯỚC 3: Validate status
        if (proposal.getStatus() != Proposal.ProposalStatus.PENDING) {
            throw new RuntimeException("Cannot withdraw proposal that is not PENDING");
        }

        // BƯỚC 4: Withdraw proposal
        proposal.setStatus(Proposal.ProposalStatus.WITHDRAWN);
        Proposal withdrawnProposal = proposalRepository.save(proposal);
        log.info("✅ Proposal withdrawn successfully: {}", proposalId);

        Job job = jobRepository.findById(proposal.getJobId()).orElse(null);
        User freelancer = userRepository.findById(freelancerId).orElse(null);

        return mapToProposalResponse(withdrawnProposal, job, freelancer);
    }
}