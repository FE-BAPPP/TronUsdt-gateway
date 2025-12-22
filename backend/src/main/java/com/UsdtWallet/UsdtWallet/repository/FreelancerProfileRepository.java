package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.FreelancerProfile;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FreelancerProfileRepository extends JpaRepository<FreelancerProfile, UUID> {
    
    // Find profile by User entity
    Optional<FreelancerProfile> findByUser(User user);
    
    // Find profile by User ID
    Optional<FreelancerProfile> findByUserId(UUID userId);
}