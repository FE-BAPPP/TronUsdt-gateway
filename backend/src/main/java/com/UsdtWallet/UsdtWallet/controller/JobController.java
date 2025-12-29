package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.request.JobCreateRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.dto.response.JobResponse;
import com.UsdtWallet.UsdtWallet.security.UserPrincipal;
import com.UsdtWallet.UsdtWallet.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobService jobService;

    /**
     * POST /api/jobs - Employer creates a new job
     */
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody JobCreateRequest request) {
        try {
            JobResponse job = jobService.createJob(userPrincipal.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("Job posted successfully", job));
        } catch (Exception e) {
            log.error("Error creating job: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<JobResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * GET /api/jobs/my-jobs - Employer gets their posted jobs
     */
    @GetMapping("/my-jobs")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Page<JobResponse>> getMyJobs(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobResponse> jobs = jobService.getEmployerJobs(userPrincipal.getId(), pageable);
        return ResponseEntity.ok(jobs);
    }

    /**
     * GET /api/jobs - Browse open jobs (for freelancers)
     */
    @GetMapping
    @PreAuthorize("hasRole('FREELANCER') or hasRole('EMPLOYER')")
    public ResponseEntity<Page<JobResponse>> browseJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobResponse> jobs = jobService.browseJobs(pageable);
        return ResponseEntity.ok(jobs);
    }

    /**
     * GET /api/jobs/search?keyword=React - Search jobs
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('EMPLOYER')")
    public ResponseEntity<Page<JobResponse>> searchJobs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobResponse> jobs = jobService.searchJobs(keyword, pageable);
        return ResponseEntity.ok(jobs);
    }

    /**
     * GET /api/jobs/{id} - Get job details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FREELANCER') or hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<JobResponse>> getJobDetails(@PathVariable UUID id) {
        try {
            JobResponse job = jobService.getJobById(id);
            return ResponseEntity.ok(ApiResponse.success(job));
        } catch (Exception e) {
            log.error("Error getting job: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<JobResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * 🔄 PUT /api/jobs/{id} - Cập nhật job (chỉ employer owner)
     * 
     * Chỉ có thể update khi job vẫn ở trạng thái OPEN
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id,
            @Valid @RequestBody JobCreateRequest request) {
        try {
            JobResponse job = jobService.updateJob(id, userPrincipal.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("Job updated successfully", job));
        } catch (Exception e) {
            log.error("Error updating job: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<JobResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * ❌ DELETE /api/jobs/{id} - Xóa job (chỉ employer owner)
     * 
     * Chỉ có thể xóa nếu:
     * - Job vẫn OPEN
     * - Chưa có proposal nào được AWARDED
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        try {
            jobService.deleteJob(id, userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Job deleted successfully")
                .build());
        } catch (Exception e) {
            log.error("Error deleting job: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * 🔒 POST /api/jobs/{id}/close - Đóng job (không nhận proposal nữa)
     * 
     * Employer có thể close job khi:
     * - Đã tìm được freelancer phù hợp
     * - Không muốn nhận proposal nữa
     */
    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<JobResponse>> closeJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        try {
            JobResponse job = jobService.closeJob(id, userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success("Job closed successfully", job));
        } catch (Exception e) {
            log.error("Error closing job: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<JobResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}