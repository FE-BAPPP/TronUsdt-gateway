package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO cho Review
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private UUID id;
    private UUID projectId;
    private UUID reviewerId;
    private String reviewerName;
    private String reviewerAvatar;
    private UUID revieweeId;
    private String revieweeName;
    private String revieweeAvatar;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    // Thông tin dự án (tùy chọn)
    private String projectTitle;
}
