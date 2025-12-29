package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.request.EmployerProfileRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.dto.response.EmployerProfileResponse;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.EmployerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 🏢 EMPLOYER PROFILE CONTROLLER
 * 
 * Quản lý profile của Employer (công ty/cá nhân thuê freelancer)
 * 
 * Endpoints:
 * - GET /api/employer/profile - Lấy profile của mình
 * - POST /api/employer/profile - Tạo profile mới
 * - PUT /api/employer/profile - Cập nhật profile
 * - GET /api/employer/profile/{employerId} - Xem public profile
 */
@RestController
@RequestMapping("/api/employer/profile")
@RequiredArgsConstructor
@Slf4j
public class EmployerProfileController {

    private final EmployerProfileService employerProfileService;

    /**
     * 📋 GET /api/employer/profile - Lấy profile của Employer hiện tại
     * 
     * @return EmployerProfileResponse hoặc 404 nếu chưa tạo
     */
    @GetMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<EmployerProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            EmployerProfileResponse profile = employerProfileService.getProfileByUserId(userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success(profile));
        } catch (RuntimeException e) {
            log.error("Error getting employer profile: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<EmployerProfileResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * ✏️ POST /api/employer/profile - Tạo profile mới cho Employer
     * 
     * Chỉ có thể tạo 1 lần. Nếu đã có profile thì dùng PUT để update.
     * 
     * @param request Thông tin profile (company_name, industry, etc.)
     * @return EmployerProfileResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<EmployerProfileResponse>> createProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody EmployerProfileRequest request) {
        try {
            EmployerProfileResponse profile = employerProfileService.createProfile(
                userPrincipal.getId(), 
                request
            );
            log.info("✅ Employer profile created for user: {}", userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success("Profile created successfully", profile));
        } catch (RuntimeException e) {
            log.error("Error creating employer profile: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<EmployerProfileResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * 🔄 PUT /api/employer/profile - Cập nhật profile Employer
     * 
     * @param request Thông tin cần update
     * @return EmployerProfileResponse đã update
     */
    @PutMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<EmployerProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody EmployerProfileRequest request) {
        try {
            EmployerProfileResponse profile = employerProfileService.updateProfile(
                userPrincipal.getId(), 
                request
            );
            log.info("✅ Employer profile updated for user: {}", userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profile));
        } catch (RuntimeException e) {
            log.error("Error updating employer profile: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<EmployerProfileResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * 👁️ GET /api/employer/profile/{employerId} - Xem public profile của Employer
     * 
     * Freelancer có thể xem để biết thông tin công ty trước khi apply job
     * 
     * @param employerId UUID của employer
     * @return EmployerProfileResponse (public view)
     */
    @GetMapping("/{employerId}")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<EmployerProfileResponse>> getPublicProfile(
            @PathVariable UUID employerId) {
        try {
            EmployerProfileResponse profile = employerProfileService.getProfileByUserId(employerId);
            return ResponseEntity.ok(ApiResponse.success(profile));
        } catch (RuntimeException e) {
            log.error("Error getting employer profile: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<EmployerProfileResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
