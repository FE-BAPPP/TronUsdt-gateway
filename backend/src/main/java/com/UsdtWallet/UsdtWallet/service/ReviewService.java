package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.dto.request.ReviewCreateRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.ReviewResponse;
import com.UsdtWallet.UsdtWallet.model.entity.*;
import com.UsdtWallet.UsdtWallet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quản lý Reviews
 * 
 * Luồng nghiệp vụ:
 * 1. Sau khi hoàn thành dự án (COMPLETED)
 * 2. Employer đánh giá Freelancer
 * 3. Freelancer đánh giá Employer
 * 4. Cập nhật điểm trung bình vào profile
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final NotificationService notificationService;

    /**
     * Tạo review sau khi hoàn thành dự án
     * 
     * @param reviewerId ID người đánh giá (Employer hoặc Freelancer)
     * @param request Thông tin review
     * @return Review vừa tạo
     */
    @Transactional
    public ReviewResponse createReview(UUID reviewerId, ReviewCreateRequest request) {
        // Validate project exists and completed
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (project.getStatus() != Project.ProjectStatus.COMPLETED) {
            throw new RuntimeException("Can only review completed projects");
        }

        // Validate reviewer is part of the project
        if (!project.getEmployerId().equals(reviewerId) && !project.getFreelancerId().equals(reviewerId)) {
            throw new RuntimeException("You are not authorized to review this project");
        }

        // Validate reviewee is the other party
        UUID expectedRevieweeId = project.getEmployerId().equals(reviewerId) 
                ? project.getFreelancerId() 
                : project.getEmployerId();

        if (!expectedRevieweeId.equals(request.getRevieweeId())) {
            throw new RuntimeException("Invalid reviewee ID");
        }

        // Check if already reviewed
        if (reviewRepository.existsByProjectIdAndReviewerId(request.getProjectId(), reviewerId)) {
            throw new RuntimeException("You have already reviewed this project");
        }

        // Validate reviewee exists
        User reviewee = userRepository.findById(request.getRevieweeId())
                .orElseThrow(() -> new RuntimeException("Reviewee not found"));

        // Create review
        Review review = Review.builder()
                .projectId(request.getProjectId())
                .reviewerId(reviewerId)
                .revieweeId(request.getRevieweeId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);
        log.info("Review created: {} -> {}, rating: {}", reviewerId, request.getRevieweeId(), request.getRating());

        // Update reviewee's average rating
        updateUserAverageRating(request.getRevieweeId());

        // Send notification
        notificationService.createNotification(
                request.getRevieweeId(),
                Notification.NotificationType.SYSTEM_ALERT,
                "New Review Received",
                "You received a " + request.getRating() + "-star review for your work.",
                "REVIEW",
                review.getId()
        );

        return mapToResponse(review);
    }

    /**
     * Cập nhật điểm trung bình của user
     */
    @Transactional
    public void updateUserAverageRating(UUID userId) {
        Double avgRating = reviewRepository.calculateAverageRating(userId);
        
        if (avgRating != null) {
            // Round to 2 decimal places
            BigDecimal roundedRating = BigDecimal.valueOf(avgRating)
                    .setScale(2, RoundingMode.HALF_UP);

            // Update freelancer profile if user is freelancer
            freelancerProfileRepository.findByUserId(userId).ifPresent(profile -> {
                profile.setAvgRating(roundedRating);
                freelancerProfileRepository.save(profile);
                log.info("Updated freelancer {} avg rating to {}", userId, roundedRating);
            });
        }
    }

    /**
     * Lấy reviews của một user (được đánh giá)
     */
    public Page<ReviewResponse> getReviewsForUser(UUID userId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId, pageable);
        return reviews.map(this::mapToResponse);
    }

    /**
     * Lấy reviews mà user đã viết
     */
    public Page<ReviewResponse> getReviewsByUser(UUID userId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByReviewerIdOrderByCreatedAtDesc(userId, pageable);
        return reviews.map(this::mapToResponse);
    }

    /**
     * Lấy reviews theo project
     */
    public List<ReviewResponse> getReviewsByProject(UUID projectId) {
        List<Review> reviews = reviewRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return reviews.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thống kê reviews của user
     */
    public ReviewStatistics getUserReviewStatistics(UUID userId) {
        Double avgRating = reviewRepository.calculateAverageRating(userId);
        long totalReviews = reviewRepository.countByRevieweeId(userId);

        // Count by rating
        long fiveStars = reviewRepository.findByRevieweeIdAndRatingOrderByCreatedAtDesc(userId, 5).size();
        long fourStars = reviewRepository.findByRevieweeIdAndRatingOrderByCreatedAtDesc(userId, 4).size();
        long threeStars = reviewRepository.findByRevieweeIdAndRatingOrderByCreatedAtDesc(userId, 3).size();
        long twoStars = reviewRepository.findByRevieweeIdAndRatingOrderByCreatedAtDesc(userId, 2).size();
        long oneStar = reviewRepository.findByRevieweeIdAndRatingOrderByCreatedAtDesc(userId, 1).size();

        return ReviewStatistics.builder()
                .userId(userId)
                .averageRating(avgRating != null ? 
                        BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalReviews(totalReviews)
                .fiveStars(fiveStars)
                .fourStars(fourStars)
                .threeStars(threeStars)
                .twoStars(twoStars)
                .oneStar(oneStar)
                .build();
    }

    /**
     * Kiểm tra xem user đã review project chưa
     */
    public boolean hasUserReviewedProject(UUID projectId, UUID userId) {
        return reviewRepository.existsByProjectIdAndReviewerId(projectId, userId);
    }

    /**
     * Map entity to response
     */
    private ReviewResponse mapToResponse(Review review) {
        User reviewer = userRepository.findById(review.getReviewerId()).orElse(null);
        User reviewee = userRepository.findById(review.getRevieweeId()).orElse(null);
        Project project = projectRepository.findById(review.getProjectId()).orElse(null);

        return ReviewResponse.builder()
                .id(review.getId())
                .projectId(review.getProjectId())
                .reviewerId(review.getReviewerId())
                .reviewerName(reviewer != null ? reviewer.getFullName() : "Unknown")
                .reviewerAvatar(reviewer != null ? reviewer.getAvatar() : null)
                .revieweeId(review.getRevieweeId())
                .revieweeName(reviewee != null ? reviewee.getFullName() : "Unknown")
                .revieweeAvatar(reviewee != null ? reviewee.getAvatar() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .projectTitle(project != null ? project.getJob().getTitle() : null)
                .build();
    }

    /**
     * Inner class for review statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class ReviewStatistics {
        private UUID userId;
        private BigDecimal averageRating;
        private long totalReviews;
        private long fiveStars;
        private long fourStars;
        private long threeStars;
        private long twoStars;
        private long oneStar;
    }
}
