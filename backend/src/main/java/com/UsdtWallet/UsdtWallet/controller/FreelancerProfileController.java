package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.request.FreelancerProfileUpdateRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.dto.response.FreelancerProfileResponse;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.FreelancerProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/freelancer/profile")
@RequiredArgsConstructor
@Slf4j
public class FreelancerProfileController {

    private final FreelancerProfileService freelancerProfileService;

    /**
     * Get current freelancer's profile
     */
    @GetMapping
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<ApiResponse<FreelancerProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            FreelancerProfileResponse profile = freelancerProfileService
                .getFreelancerProfile(userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success(profile));
        } catch (Exception e) {
            log.error("Failed to get freelancer profile", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<FreelancerProfileResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Update freelancer profile
     */
    @PutMapping
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<ApiResponse<FreelancerProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody FreelancerProfileUpdateRequest request) {
        try {
            FreelancerProfileResponse profile = freelancerProfileService
                .updateFreelancerProfile(userPrincipal.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profile));
        } catch (Exception e) {
            log.error("Failed to update freelancer profile", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<FreelancerProfileResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * 🆕 Get public profile of a freelancer (viewable by employers)
     */
    @GetMapping("/{freelancerId}")
    @PreAuthorize("hasRole('EMPLOYER') or hasRole('FREELANCER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FreelancerProfileResponse>> getPublicProfile(
            @PathVariable UUID freelancerId) {
        try {
            FreelancerProfileResponse profile = freelancerProfileService
                .getFreelancerProfile(freelancerId);
            return ResponseEntity.ok(ApiResponse.success(profile));
        } catch (Exception e) {
            log.error("Failed to get freelancer public profile: {}", freelancerId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<FreelancerProfileResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}