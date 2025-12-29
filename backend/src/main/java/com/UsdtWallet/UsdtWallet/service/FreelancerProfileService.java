package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.dto.request.FreelancerProfileUpdateRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.FreelancerProfileResponse;
import com.UsdtWallet.UsdtWallet.model.entity.FreelancerProfile;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import com.UsdtWallet.UsdtWallet.repository.FreelancerProfileRepository;
import com.UsdtWallet.UsdtWallet.repository.ProjectRepository;
import com.UsdtWallet.UsdtWallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreelancerProfileService {

    private final FreelancerProfileRepository freelancerProfileRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    /**
     * Get freelancer profile by user ID
     */
    public FreelancerProfileResponse getFreelancerProfile(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        FreelancerProfile profile = freelancerProfileRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Freelancer profile not found"));

        return mapToResponse(profile);
    }

    /**
     * Update freelancer profile
     */
    @Transactional
    public FreelancerProfileResponse updateFreelancerProfile(UUID userId, FreelancerProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        FreelancerProfile profile = freelancerProfileRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Freelancer profile not found"));

        // Update fields
        if (request.getProfessionalTitle() != null) {
            profile.setProfessionalTitle(request.getProfessionalTitle());
        }

        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }

        if (request.getHourlyRate() != null) {
            profile.setHourlyRate(request.getHourlyRate());
        }

        if (request.getAvailability() != null) {
            profile.setAvailability(request.getAvailability());
        }

        if (request.getPortfolioUrl() != null) {
            profile.setPortfolioUrl(request.getPortfolioUrl());
        }

        if (request.getLinkedinUrl() != null) {
            profile.setLinkedinUrl(request.getLinkedinUrl());
        }

        if (request.getGithubUrl() != null) {
            profile.setGithubUrl(request.getGithubUrl());
        }

        profile.setUpdatedAt(LocalDateTime.now());
        FreelancerProfile saved = freelancerProfileRepository.save(profile);

        log.info("Freelancer profile updated for user: {}", userId);

        return mapToResponse(saved);
    }

    /**
     * Map FreelancerProfile entity to response DTO
     */
    private FreelancerProfileResponse mapToResponse(FreelancerProfile profile) {
        User user = profile.getUser();
        
        // Count active projects for this freelancer
        long activeProjects = projectRepository.countByFreelancerIdAndStatus(
            user.getId(), 
            com.UsdtWallet.UsdtWallet.model.entity.Project.ProjectStatus.IN_PROGRESS
        );

        return FreelancerProfileResponse.builder()
            .id(profile.getId())
            .userId(user.getId())
            .userName(user.getFullName() != null ? user.getFullName() : user.getUsername())
            .userEmail(user.getEmail())
            .avatar(user.getAvatar())
            .professionalTitle(profile.getProfessionalTitle())
            .bio(profile.getBio())
            .hourlyRate(profile.getHourlyRate())
            .availability(profile.getAvailability())
            .portfolioUrl(profile.getPortfolioUrl())
            .linkedinUrl(profile.getLinkedinUrl())
            .githubUrl(profile.getGithubUrl())
            .totalEarnings(profile.getTotalEarnings() != null ? profile.getTotalEarnings() : BigDecimal.ZERO)
            .jobsCompleted(profile.getJobsCompleted() != null ? profile.getJobsCompleted() : 0)
            .avgRating(profile.getAvgRating() != null ? profile.getAvgRating().doubleValue() : 0.0)
            .activeProjects((int) activeProjects)
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .build();
    }

    /**
     * 📈 INCREMENT JOBS COMPLETED - Called after project completion
     */
    @Transactional
    public void incrementJobsCompleted(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        
        freelancerProfileRepository.findByUser(user).ifPresent(profile -> {
            profile.setJobsCompleted((profile.getJobsCompleted() != null ? profile.getJobsCompleted() : 0) + 1);
            freelancerProfileRepository.save(profile);
            log.debug("📈 Incremented jobs_completed for freelancer: {}", userId);
        });
    }

    /**
     * 💰 ADD TO TOTAL EARNINGS - Called after milestone payment release
     */
    @Transactional
    public void addToTotalEarnings(UUID userId, BigDecimal amount) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        
        freelancerProfileRepository.findByUser(user).ifPresent(profile -> {
            BigDecimal currentEarnings = profile.getTotalEarnings() != null ? profile.getTotalEarnings() : BigDecimal.ZERO;
            profile.setTotalEarnings(currentEarnings.add(amount));
            freelancerProfileRepository.save(profile);
            log.debug("💰 Added {} to total_earnings for freelancer: {}", amount, userId);
        });
    }

    /**
     * ⭐ UPDATE AVERAGE RATING - Called after new review
     */
    @Transactional
    public void updateAverageRating(UUID userId, BigDecimal newRating, int totalReviews) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        
        freelancerProfileRepository.findByUser(user).ifPresent(profile -> {
            BigDecimal currentAvg = profile.getAvgRating() != null ? profile.getAvgRating() : BigDecimal.ZERO;
            BigDecimal currentTotal = currentAvg.multiply(BigDecimal.valueOf(totalReviews - 1));
            BigDecimal newTotal = currentTotal.add(newRating);
            BigDecimal newAvg = newTotal.divide(BigDecimal.valueOf(totalReviews), 2, java.math.RoundingMode.HALF_UP);
            
            profile.setAvgRating(newAvg);
            freelancerProfileRepository.save(profile);
            log.debug("⭐ Updated avg_rating to {} for freelancer: {}", newAvg, userId);
        });
    }
}