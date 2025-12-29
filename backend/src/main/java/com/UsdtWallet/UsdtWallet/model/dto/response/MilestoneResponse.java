package com.UsdtWallet.UsdtWallet.model.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneResponse {
    private UUID id;
    private UUID projectId;
    private String title;
    private String description;
    private BigDecimal amount;
    private String currency;
    private Integer sequenceOrder;
    private String status;
    private LocalDateTime dueDate;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime releasedAt;
    private UUID approvedBy;
    private String approvedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // ✅ NEW: Milestone workflow fields
    private String deliverables;      // Freelancer's deliverable description
    private String completionNotes;   // Freelancer's completion notes
    private String rejectionReason;   // Employer's rejection feedback
    private List<FileResponse> attachments; // Attached files
}