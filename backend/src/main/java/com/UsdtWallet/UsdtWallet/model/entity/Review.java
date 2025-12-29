package com.UsdtWallet.UsdtWallet.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Review Entity - Đánh giá sau khi hoàn thành dự án
 * 
 * Mối quan hệ:
 * - ManyToOne với Project (nhiều review cho 1 project)
 * - ManyToOne với User (reviewer - người đánh giá)
 * - ManyToOne với User (reviewee - người được đánh giá)
 * 
 * Ràng buộc:
 * - Mỗi cặp (project, reviewer, reviewee) chỉ có 1 review duy nhất
 * - Rating từ 1-5 sao
 */
@Entity
@Table(name = "reviews", 
    uniqueConstraints = @UniqueConstraint(
        name = "uq_review_per_project", 
        columnNames = {"project_id", "reviewer_id", "reviewee_id"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * ID của dự án được đánh giá
     */
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    /**
     * ID của người đánh giá (Employer hoặc Freelancer)
     */
    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    /**
     * ID của người được đánh giá (Freelancer hoặc Employer)
     */
    @Column(name = "reviewee_id", nullable = false)
    private UUID revieweeId;

    /**
     * Điểm đánh giá (1-5 sao)
     */
    @Column(nullable = false)
    private Integer rating;

    /**
     * Nhận xét chi tiết (tùy chọn)
     */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /**
     * Thời gian tạo review
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", insertable = false, updatable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", insertable = false, updatable = false)
    private User reviewee;
}
