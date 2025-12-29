package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.request.ReviewCreateRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.ReviewResponse;
import com.UsdtWallet.UsdtWallet.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý Reviews
 * Endpoint: /api/reviews
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "API quản lý đánh giá sau dự án")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Tạo review sau khi hoàn thành dự án
     * POST /api/reviews
     */
    @PostMapping
    @Operation(summary = "Tạo review", description = "Employer hoặc Freelancer đánh giá nhau sau khi hoàn thành dự án")
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy reviews của một user (được đánh giá)
     * GET /api/reviews/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy reviews của user", description = "Xem tất cả reviews mà user này nhận được")
    public ResponseEntity<Page<ReviewResponse>> getReviewsForUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponse> reviews = reviewService.getReviewsForUser(userId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Lấy reviews mà user đã viết
     * GET /api/reviews/by-user/{userId}
     */
    @GetMapping("/by-user/{userId}")
    @Operation(summary = "Lấy reviews do user viết", description = "Xem tất cả reviews mà user này đã viết cho người khác")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponse> reviews = reviewService.getReviewsByUser(userId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Lấy reviews theo project
     * GET /api/reviews/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    @Operation(summary = "Lấy reviews của project", description = "Xem tất cả reviews liên quan đến project này")
    public ResponseEntity<List<ReviewResponse>> getReviewsByProject(
            @PathVariable UUID projectId
    ) {
        List<ReviewResponse> reviews = reviewService.getReviewsByProject(projectId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Lấy thống kê reviews của user
     * GET /api/reviews/statistics/{userId}
     */
    @GetMapping("/statistics/{userId}")
    @Operation(summary = "Thống kê reviews", description = "Lấy thống kê chi tiết về reviews của user (trung bình, phân bố sao)")
    public ResponseEntity<ReviewService.ReviewStatistics> getUserReviewStatistics(
            @PathVariable UUID userId
    ) {
        ReviewService.ReviewStatistics statistics = reviewService.getUserReviewStatistics(userId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Lấy reviews của chính mình (user đang login)
     * GET /api/reviews/my-reviews
     */
    @GetMapping("/my-reviews")
    @Operation(summary = "Lấy reviews của tôi", description = "Xem tất cả reviews mà tôi nhận được")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponse> reviews = reviewService.getReviewsForUser(userId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Kiểm tra đã review project chưa
     * GET /api/reviews/check/{projectId}
     */
    @GetMapping("/check/{projectId}")
    @Operation(summary = "Kiểm tra đã review", description = "Kiểm tra xem user đã review project này chưa")
    public ResponseEntity<Boolean> hasReviewedProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        boolean hasReviewed = reviewService.hasUserReviewedProject(projectId, userId);
        return ResponseEntity.ok(hasReviewed);
    }
}
