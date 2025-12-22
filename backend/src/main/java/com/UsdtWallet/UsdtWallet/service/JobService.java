package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.dto.request.JobCreateRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.JobResponse;
import com.UsdtWallet.UsdtWallet.model.entity.Job;
import com.UsdtWallet.UsdtWallet.model.entity.Skill;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import com.UsdtWallet.UsdtWallet.repository.JobRepository;
import com.UsdtWallet.UsdtWallet.repository.SkillRepository;
import com.UsdtWallet.UsdtWallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final PointsService pointsService;

    /**
     * Employer posts a new job
     */
    @Transactional
    public JobResponse createJob(UUID employerId, JobCreateRequest request) {
        // Validate employer exists
        User employer = userRepository.findById(employerId)
            .orElseThrow(() -> new RuntimeException("Employer not found"));

        // Validate budget range
        if (request.getBudgetMin() != null && request.getBudgetMax() != null) {
            if (request.getBudgetMin().compareTo(request.getBudgetMax()) > 0) {
                throw new RuntimeException("Budget min cannot be greater than budget max");
            }
        }

        // ✅ FIX: Use main budget field for validation
        BigDecimal requiredBudget = request.getBudget();
        
        BigDecimal availableBalance = pointsService.getAvailableBalance(employerId);
        if (availableBalance.compareTo(requiredBudget) < 0) {
            throw new RuntimeException("Insufficient balance to post this job. Required: " + 
                requiredBudget + " PTS, Available: " + availableBalance + " PTS");
        }

        // Process skills
        Set<Skill> jobSkills = new HashSet<>();
        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            for (String skillIdStr : request.getSkillIds()) {
                try {
                    UUID skillId = UUID.fromString(skillIdStr);
                    Skill skill = skillRepository.findById(skillId)
                        .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));
                    jobSkills.add(skill);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid skill UUID: {}", skillIdStr);
                }
            }
        }

        // ✅ FIX: Create job with all required fields
        Job.ProjectType projectType = Job.ProjectType.valueOf(request.getType());
        
        Job job = Job.builder()
            .employerId(employerId)
            .title(request.getTitle())
            .description(request.getDescription())
            .jobType(projectType)  // old field
            .type(projectType)     // ✅ new field (required)
            .budget(request.getBudget())  // ✅ main budget (required)
            .budgetMin(request.getBudgetMin())
            .budgetMax(request.getBudgetMax())
            .currency(request.getCurrency() != null ? request.getCurrency() : "USDT")
            .duration(request.getDuration())  // ✅ new field
            .deadline(request.getDeadline())
            .status(Job.JobStatus.OPEN)
            .requiredSkills(jobSkills)
            .build();

        Job savedJob = jobRepository.save(job);
        log.info("Job created: {} by employer: {}", savedJob.getId(), employerId);

        return mapToJobResponse(savedJob);
    }

    /**
     * Get jobs posted by employer
     */
    public Page<JobResponse> getEmployerJobs(UUID employerId, Pageable pageable) {
        return jobRepository.findByEmployerIdOrderByCreatedAtDesc(employerId, pageable)
            .map(this::mapToJobResponse);
    }

    /**
     * Browse open jobs (for freelancers)
     */
    public Page<JobResponse> browseJobs(Pageable pageable) {
        return jobRepository.findByStatusOrderByCreatedAtDesc(Job.JobStatus.OPEN, pageable)
            .map(this::mapToJobResponse);
    }

    /**
     * Search jobs by keyword
     */
    public Page<JobResponse> searchJobs(String keyword, Pageable pageable) {
        return jobRepository.searchJobs(Job.JobStatus.OPEN, keyword, pageable)
            .map(this::mapToJobResponse);
    }

    /**
     * Get job details
     */
    public JobResponse getJobById(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
        return mapToJobResponse(job);
    }

    /**
     * Map Job entity to JobResponse DTO
     */
    private JobResponse mapToJobResponse(Job job) {
        User employer = userRepository.findById(job.getEmployerId()).orElse(null);
        
        return JobResponse.builder()
            .id(job.getId())
            .employerId(job.getEmployerId())
            .employerName(employer != null ? employer.getFullName() : "Unknown")
            .title(job.getTitle())
            .description(job.getDescription())
            .type(job.getType().name())
            .budget(job.getBudget())  
            .budgetMin(job.getBudgetMin())
            .budgetMax(job.getBudgetMax())
            .currency(job.getCurrency())
            .duration(job.getDuration())  
            .deadline(job.getDeadline())
            .status(job.getStatus().name())
            .skills(job.getRequiredSkills().stream()
                .map(Skill::getName)
                .collect(Collectors.toSet()))
            .proposalCount(0) // TODO: Count proposals later
            .createdAt(job.getCreatedAt())
            .updatedAt(job.getUpdatedAt())
            .build();
    }
}