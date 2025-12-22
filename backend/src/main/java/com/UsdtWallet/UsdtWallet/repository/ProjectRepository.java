package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    
    // Find projects by employer
    Page<Project> findByEmployerIdOrderByCreatedAtDesc(UUID employerId, Pageable pageable);
    
    // Find projects by freelancer
    Page<Project> findByFreelancerIdOrderByCreatedAtDesc(UUID freelancerId, Pageable pageable);
    
    // Find project by job
    Optional<Project> findByJobId(UUID jobId);
    
    // Count active projects for freelancer
    long countByFreelancerIdAndStatus(UUID freelancerId, Project.ProjectStatus status);
}