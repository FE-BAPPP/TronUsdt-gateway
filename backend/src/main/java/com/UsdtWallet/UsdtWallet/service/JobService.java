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
    private final EmployerProfileService employerProfileService;

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
        
        // ✅ UPDATE employer profile stats
        employerProfileService.incrementJobsPosted(employerId);

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

    /**
     * 🔄 UPDATE JOB - Employer cập nhật job của mình
     * 
     * BƯỚC 1: Validate job tồn tại
     * BƯỚC 2: Validate employer là owner
     * BƯỚC 3: Validate job vẫn OPEN (không thể update job đã CLOSED)
     * BƯỚC 4: Update các field
     */
    @Transactional
    public JobResponse updateJob(UUID jobId, UUID employerId, JobCreateRequest request) {
        log.info("🔄 Updating job: {} by employer: {}", jobId, employerId);

        // BƯỚC 1: Validate job exists
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));

        // BƯỚC 2: Validate ownership
        if (!job.getEmployerId().equals(employerId)) {
            throw new RuntimeException("You are not authorized to update this job");
        }

        // BƯỚC 3: Validate status
        if (job.getStatus() != Job.JobStatus.OPEN) {
            throw new RuntimeException("Cannot update job that is not OPEN");
        }

        // BƯỚC 4: Update fields
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setBudget(request.getBudget());
        job.setBudgetMin(request.getBudgetMin());
        job.setBudgetMax(request.getBudgetMax());
        job.setDuration(request.getDuration());
        job.setDeadline(request.getDeadline());

        // Update skills if provided
        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            Set<Skill> updatedSkills = new HashSet<>();
            for (String skillIdStr : request.getSkillIds()) {
                try {
                    UUID skillId = UUID.fromString(skillIdStr);
                    Skill skill = skillRepository.findById(skillId)
                        .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));
                    updatedSkills.add(skill);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid skill UUID: {}", skillIdStr);
                }
            }
            job.setRequiredSkills(updatedSkills);
        }

        Job updatedJob = jobRepository.save(job);
        log.info("✅ Job updated successfully: {}", jobId);

        return mapToJobResponse(updatedJob);
    }

    /**
     * ❌ DELETE JOB - Employer xóa job
     * 
     * BƯỚC 1: Validate job tồn tại
     * BƯỚC 2: Validate employer là owner
     * BƯỚC 3: Validate chưa có proposal nào được AWARDED
     * BƯỚC 4: Xóa job
     */
    @Transactional
    public void deleteJob(UUID jobId, UUID employerId) {
        log.info("❌ Deleting job: {} by employer: {}", jobId, employerId);

        // BƯỚC 1: Validate job exists
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));

        // BƯỚC 2: Validate ownership
        if (!job.getEmployerId().equals(employerId)) {
            throw new RuntimeException("You are not authorized to delete this job");
        }

        // BƯỚC 3: Check if any proposal was awarded
        // (Nếu đã award thì có project rồi, không được xóa)
        if (job.getStatus() == Job.JobStatus.IN_PROGRESS) {
            throw new RuntimeException("Cannot delete job that has an active project");
        }

        // BƯỚC 4: Delete job (cascade sẽ xóa proposals và conversations liên quan)
        jobRepository.delete(job);
        log.info("✅ Job deleted successfully: {}", jobId);
    }

    /**
     * 🔒 CLOSE JOB - Employer đóng job (không nhận proposal nữa)
     * 
     * BƯỚC 1: Validate job tồn tại
     * BƯỚC 2: Validate employer là owner
     * BƯỚC 3: Validate job đang OPEN
     * BƯỚC 4: Đổi status thành CLOSED
     */
    @Transactional
    public JobResponse closeJob(UUID jobId, UUID employerId) {
        log.info("🔒 Closing job: {} by employer: {}", jobId, employerId);

        // BƯỚC 1: Validate job exists
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));

        // BƯỚC 2: Validate ownership
        if (!job.getEmployerId().equals(employerId)) {
            throw new RuntimeException("You are not authorized to close this job");
        }

        // BƯỚC 3: Validate status
        if (job.getStatus() != Job.JobStatus.OPEN) {
            throw new RuntimeException("Job is already closed or in progress");
        }

        // BƯỚC 4: Close job
        job.setStatus(Job.JobStatus.CLOSED);
        Job closedJob = jobRepository.save(job);
        log.info("✅ Job closed successfully: {}", jobId);

        return mapToJobResponse(closedJob);
    }
}