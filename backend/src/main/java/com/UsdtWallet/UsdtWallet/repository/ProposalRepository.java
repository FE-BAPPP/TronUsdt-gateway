package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.Proposal;
import com.UsdtWallet.UsdtWallet.model.entity.Proposal.ProposalStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, UUID> {
    
    // Check if freelancer already submitted proposal for this job
    boolean existsByJobIdAndFreelancerId(UUID jobId, UUID freelancerId);
    
    // Get proposals for a job
    Page<Proposal> findByJobIdOrderByCreatedAtDesc(UUID jobId, Pageable pageable);
    
    // Get freelancer's proposals
    Page<Proposal> findByFreelancerIdOrderByCreatedAtDesc(UUID freelancerId, Pageable pageable);
    
    // Count proposals for a job
    long countByJobId(UUID jobId);

    List<Proposal> findByJobIdAndStatus(UUID jobId, ProposalStatus status);
}