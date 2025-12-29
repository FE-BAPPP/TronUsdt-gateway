package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.entity.EmployerProfile;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import com.UsdtWallet.UsdtWallet.model.dto.request.EmployerProfileRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.EmployerProfileResponse;
import com.UsdtWallet.UsdtWallet.repository.EmployerProfileRepository;
import com.UsdtWallet.UsdtWallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 🏢 EMPLOYER PROFILE SERVICE
 * 
 * Quản lý profile của Employer - công ty/cá nhân thuê freelancer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployerProfileService {

    private final EmployerProfileRepository employerProfileRepository;
    private final UserRepository userRepository;

    /**
     * 📋 Lấy profile của Employer theo userId
     */
    @Transactional(readOnly = true)
    public EmployerProfileResponse getProfileByUserId(UUID userId) {
        EmployerProfile profile = employerProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Employer profile not found"));
        
        return mapToResponse(profile);
    }

    /**
     * ✏️ Tạo profile mới cho Employer
     * 
     * BƯỚC 1: Validate user tồn tại và có role EMPLOYER
     * BƯỚC 2: Check chưa có profile (1 user chỉ có 1 profile)
     * BƯỚC 3: Tạo profile mới
     * BƯỚC 4: Lưu vào database
     */
    @Transactional
    public EmployerProfileResponse createProfile(UUID userId, EmployerProfileRequest request) {
        log.info("🏢 Creating employer profile for user: {}", userId);

        // BƯỚC 1: Validate user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // BƯỚC 2: Check chưa có profile
        if (employerProfileRepository.existsByUserId(userId)) {
            throw new RuntimeException("Employer profile already exists. Use PUT to update.");
        }

        // BƯỚC 3: Tạo profile mới
        EmployerProfile profile = EmployerProfile.builder()
            .userId(userId)
            .companyName(request.getCompanyName())
            .companyWebsite(request.getCompanyWebsite())
            .companySize(request.getCompanySize())
            .industry(request.getIndustry())
            .jobsPosted(0)
            .activeProjects(0)
            .totalSpent(BigDecimal.ZERO)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // BƯỚC 4: Lưu database
        profile = employerProfileRepository.save(profile);
        log.info("✅ Employer profile created successfully: {}", profile.getId());

        return mapToResponse(profile);
    }

    /**
     * 🔄 Cập nhật profile Employer
     */
    @Transactional
    public EmployerProfileResponse updateProfile(UUID userId, EmployerProfileRequest request) {
        log.info("🔄 Updating employer profile for user: {}", userId);

        EmployerProfile profile = employerProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Employer profile not found. Create profile first."));

        // Cập nhật các field
        profile.setCompanyName(request.getCompanyName());
        profile.setCompanyWebsite(request.getCompanyWebsite());
        profile.setCompanySize(request.getCompanySize());
        profile.setIndustry(request.getIndustry());
        profile.setUpdatedAt(LocalDateTime.now());

        profile = employerProfileRepository.save(profile);
        log.info("✅ Employer profile updated successfully: {}", profile.getId());

        return mapToResponse(profile);
    }

    /**
     * 📈 Cập nhật thống kê khi employer post job hoặc start project
     */
    @Transactional
    public void incrementJobsPosted(UUID userId) {
        employerProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setJobsPosted(profile.getJobsPosted() + 1);
            profile.setUpdatedAt(LocalDateTime.now());
            employerProfileRepository.save(profile);
            log.debug("📈 Incremented jobs_posted for employer: {}", userId);
        });
    }

    @Transactional
    public void incrementActiveProjects(UUID userId) {
        employerProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setActiveProjects(profile.getActiveProjects() + 1);
            profile.setUpdatedAt(LocalDateTime.now());
            employerProfileRepository.save(profile);
            log.debug("📈 Incremented active_projects for employer: {}", userId);
        });
    }

    @Transactional
    public void decrementActiveProjects(UUID userId) {
        employerProfileRepository.findByUserId(userId).ifPresent(profile -> {
            if (profile.getActiveProjects() > 0) {
                profile.setActiveProjects(profile.getActiveProjects() - 1);
                profile.setUpdatedAt(LocalDateTime.now());
                employerProfileRepository.save(profile);
                log.debug("📉 Decremented active_projects for employer: {}", userId);
            }
        });
    }

    @Transactional
    public void addToTotalSpent(UUID userId, BigDecimal amount) {
        employerProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setTotalSpent(profile.getTotalSpent().add(amount));
            profile.setUpdatedAt(LocalDateTime.now());
            employerProfileRepository.save(profile);
            log.debug("💰 Added {} to total_spent for employer: {}", amount, userId);
        });
    }

    /**
     * 🔄 Convert Entity → DTO
     */
    private EmployerProfileResponse mapToResponse(EmployerProfile profile) {
        return EmployerProfileResponse.builder()
            .id(profile.getId())
            .userId(profile.getUserId())
            .companyName(profile.getCompanyName())
            .companyWebsite(profile.getCompanyWebsite())
            .companySize(profile.getCompanySize())
            .industry(profile.getIndustry())
            .jobsPosted(profile.getJobsPosted())
            .activeProjects(profile.getActiveProjects())
            .totalSpent(profile.getTotalSpent())
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .build();
    }
}
