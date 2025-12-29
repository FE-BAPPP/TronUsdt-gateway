package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    
    // ✅ Find projects by employer with Job eagerly fetched
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.job WHERE p.employerId = :employerId ORDER BY p.createdAt DESC")
    Page<Project> findByEmployerIdOrderByCreatedAtDesc(@Param("employerId") UUID employerId, Pageable pageable);
    
    // ✅ Find projects by freelancer with Job eagerly fetched
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.job WHERE p.freelancerId = :freelancerId ORDER BY p.createdAt DESC")
    Page<Project> findByFreelancerIdOrderByCreatedAtDesc(@Param("freelancerId") UUID freelancerId, Pageable pageable);
    
    // Find project by job
    Optional<Project> findByJobId(UUID jobId);
    
    // ✅ Find project by ID with Job eagerly fetched
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.job WHERE p.id = :id")
    Optional<Project> findByIdWithJob(@Param("id") UUID id);
    
    // Count active projects for freelancer
    long countByFreelancerIdAndStatus(UUID freelancerId, Project.ProjectStatus status);
    
    // Find projects by status and user (employer or freelancer)
    @Query("SELECT p FROM Project p WHERE p.status = :status AND (p.employerId = :employerId OR p.freelancerId = :freelancerId) ORDER BY p.createdAt DESC")
    Page<Project> findByStatusAndEmployerIdOrFreelancerId(
            @Param("status") Project.ProjectStatus status,
            @Param("employerId") UUID employerId,
            @Param("freelancerId") UUID freelancerId,
            Pageable pageable
    );
    
    // Statistics methods
    long countByStatus(Project.ProjectStatus status);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByCompletedAtBetween(LocalDateTime start, LocalDateTime end);
}