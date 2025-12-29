package com.UsdtWallet.UsdtWallet.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO để tạo review sau khi hoàn thành dự án
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateRequest {

    /**
     * ID của dự án
     */
    @NotNull(message = "Project ID is required")
    private UUID projectId;

    /**
     * ID của người được đánh giá
     */
    @NotNull(message = "Reviewee ID is required")
    private UUID revieweeId;

    /**
     * Điểm đánh giá (1-5 sao)
     */
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    /**
     * Nhận xét (tùy chọn)
     */
    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;
}
