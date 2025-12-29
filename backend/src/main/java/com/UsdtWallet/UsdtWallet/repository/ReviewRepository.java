package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /**
     * Tìm review theo project và người đánh giá
     */
    Optional<Review> findByProjectIdAndReviewerId(UUID projectId, UUID reviewerId);

    /**
     * Kiểm tra xem đã có review chưa
     */
    boolean existsByProjectIdAndReviewerId(UUID projectId, UUID reviewerId);

    /**
     * Lấy tất cả reviews của một người (được đánh giá)
     */
    Page<Review> findByRevieweeIdOrderByCreatedAtDesc(UUID revieweeId, Pageable pageable);

    /**
     * Lấy tất cả reviews mà một người đã viết
     */
    Page<Review> findByReviewerIdOrderByCreatedAtDesc(UUID reviewerId, Pageable pageable);

    /**
     * Lấy reviews theo project
     */
    List<Review> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    /**
     * Tính điểm trung bình của một người
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.revieweeId = :userId")
    Double calculateAverageRating(@Param("userId") UUID userId);

    /**
     * Đếm số lượng reviews của một người
     */
    long countByRevieweeId(UUID revieweeId);

    /**
     * Lấy reviews theo rating
     */
    List<Review> findByRevieweeIdAndRatingOrderByCreatedAtDesc(UUID revieweeId, Integer rating);
}
