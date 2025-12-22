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
    private final NotificationService notificationService;
    private final PointsService pointsService; // Add PointsService

    /**
     * Freelancer submits a proposal for a job
     */
    @Transactional
    public ProposalResponse submitProposal(UUID freelancerId, ProposalCreateRequest request) {
        // 1. Validate job exists and is OPEN
        Job job = jobRepository.findById(request.getJobId())
            .orElseThrow(() -> new RuntimeException("Job not found"));

        if (job.getStatus() != Job.JobStatus.OPEN) {
            throw new RuntimeException("Job is no longer open for proposals");
        }

        // 2. Check if freelancer already submitted proposal for this job
        boolean alreadySubmitted = proposalRepository.existsByJobIdAndFreelancerId(
            request.getJobId(), freelancerId);
        
        if (alreadySubmitted) {
            throw new RuntimeException("You have already submitted a proposal for this job");
        }

        // 3. Validate freelancer exists
        User freelancer = userRepository.findById(freelancerId)
            .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        if (freelancer.getRole() != User.Role.FREELANCER) {
            throw new RuntimeException("Only freelancers can submit proposals");
        }

        // 4. Create proposal
        Proposal proposal = Proposal.builder()
            .jobId(request.getJobId())
            .freelancerId(freelancerId)
            .coverLetter(request.getCoverLetter())
            .proposedAmount(request.getProposedAmount())
            .estimatedDurationDays(request.getEstimatedDurationDays())
            .status(Proposal.ProposalStatus.PENDING)
            .build();

        Proposal savedProposal = proposalRepository.save(proposal);
        log.info("✅ Proposal submitted: {} for job: {} by freelancer: {}", 
            savedProposal.getId(), job.getId(), freelancerId);


        return mapToProposalResponse(savedProposal, job, freelancer);
    }

    /**
     * Get proposals for a job (Employer view)
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
     * Award a proposal (Employer action) - UPDATED with Project & Escrow creation
     */
    @Transactional
    public ProposalResponse awardProposal(UUID proposalId, UUID employerId) {
        // 1. Get proposal
        Proposal proposal = proposalRepository.findById(proposalId)
            .orElseThrow(() -> new RuntimeException("Proposal not found"));
        
        if (proposal.getStatus() != Proposal.ProposalStatus.PENDING) {
            throw new RuntimeException("Only pending proposals can be awarded");
        }
        
        // 2. Validate employer owns the job
        Job job = jobRepository.findById(proposal.getJobId())
            .orElseThrow(() -> new RuntimeException("Job not found"));
        
        if (!job.getEmployerId().equals(employerId)) {
            throw new RuntimeException("You are not authorized to award this proposal");
        }
        
        // 3. Check employer has sufficient balance
        BigDecimal availableBalance = pointsService.getAvailableBalance(employerId);
        if (availableBalance.compareTo(proposal.getProposedAmount()) < 0) {
            throw new RuntimeException("Insufficient balance. Available: " + availableBalance + " USDT");
        }
        
        // 4. 🆕 CREATE PROJECT
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
        log.info("✅ Project created: {} for job: {}", savedProject.getId(), job.getId());
        
        // 5. 🆕 CREATE ESCROW & LOCK FUNDS
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
        log.info("✅ Escrow created and funds locked: {} USDT for project: {}", 
            proposal.getProposedAmount(), savedProject.getId());
        
        // 6. Lock points in ledger (PENDING transaction)
        boolean locked = pointsService.lockPointsForProject(
            employerId, 
            proposal.getProposedAmount(), 
            savedProject.getId().toString()
        );
        
        if (!locked) {
            throw new RuntimeException("Failed to lock funds in escrow");
        }
        
        // 7. 🆕 CREATE DEFAULT MILESTONE
        Milestone milestone = Milestone.builder()
            .projectId(savedProject.getId())
            .title("Project Completion")
            .description("Full payment upon project completion")
            .amount(proposal.getProposedAmount())
            .currency("USDT") // 🆕 Add currency
            .sequenceOrder(1) // 🆕 Set sequence order
            .status(Milestone.MilestoneStatus.PENDING)
            .dueDate(LocalDateTime.now().plusDays(proposal.getEstimatedDurationDays()))
            .build();
        
        milestoneRepository.save(milestone);
        log.info("✅ Default milestone created for project: {}", savedProject.getId());
        
        // 8. Update proposal status
        proposal.setStatus(Proposal.ProposalStatus.AWARDED);
        proposal.setAcceptedAt(LocalDateTime.now());
        Proposal savedProposal = proposalRepository.save(proposal);
        
        // 9. Update job status
        job.setStatus(Job.JobStatus.IN_PROGRESS);
        jobRepository.save(job);
        
        // 10. Reject other pending proposals
        proposalRepository.findByJobIdAndStatus(job.getId(), Proposal.ProposalStatus.PENDING)
            .forEach(p -> {
                p.setStatus(Proposal.ProposalStatus.REJECTED);
                proposalRepository.save(p);
            });
        
        log.info("✅ Proposal awarded with escrow & milestone: {} for job: {}", 
            proposalId, job.getId());
        
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
}