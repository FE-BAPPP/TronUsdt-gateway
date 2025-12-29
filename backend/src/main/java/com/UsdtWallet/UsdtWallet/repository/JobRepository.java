package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    
    // Statistics methods
    long countByStatus(Job.JobStatus status);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Find jobs by employer
    Page<Job> findByEmployerIdOrderByCreatedAtDesc(UUID employerId, Pageable pageable);

    // Find open jobs for freelancers to browse
    Page<Job> findByStatusOrderByCreatedAtDesc(Job.JobStatus status, Pageable pageable);

    // Search jobs by title/description keyword
    @Query("SELECT j FROM Job j WHERE j.status = :status AND " +
           "(LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Job> searchJobs(@Param("status") Job.JobStatus status, 
                         @Param("keyword") String keyword, 
                         Pageable pageable);

    // Filter jobs by budget range
    // Find jobs where budgetMax falls within the range (or budgetMin if budgetMax is null)
    @Query("SELECT j FROM Job j WHERE j.status = :status AND " +
           "(j.budgetMax BETWEEN :minBudget AND :maxBudget OR " +
           "(j.budgetMax IS NULL AND j.budgetMin BETWEEN :minBudget AND :maxBudget))")
    Page<Job> findByBudgetRange(@Param("status") Job.JobStatus status,
                                @Param("minBudget") BigDecimal minBudget,
                                @Param("maxBudget") BigDecimal maxBudget,
                                Pageable pageable);

    // Count active jobs by employer
    long countByEmployerIdAndStatus(UUID employerId, Job.JobStatus status);
}