package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.response.EscrowResponse;
import com.UsdtWallet.UsdtWallet.service.EscrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý Escrow
 * Endpoint: /api/escrow
 */
@RestController
@RequestMapping("/api/escrow")
@RequiredArgsConstructor
@Tag(name = "Escrow", description = "API quản lý ký quỹ (Escrow)")
public class EscrowController {

    private final EscrowService escrowService;

    /**
     * Lock funds vào escrow khi tạo milestone
     * POST /api/escrow/lock/{milestoneId}
     */
    @PostMapping("/lock/{milestoneId}")
    @Operation(summary = "Lock funds", description = "Employer lock funds vào escrow cho milestone")
    public ResponseEntity<EscrowResponse> lockFunds(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID milestoneId
    ) {
        UUID employerId = UUID.fromString(userDetails.getUsername());
        EscrowResponse response = escrowService.lockFundsForMilestone(milestoneId, employerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Release funds từ escrow khi milestone approved
     * POST /api/escrow/release/{milestoneId}
     */
    @PostMapping("/release/{milestoneId}")
    @Operation(summary = "Release funds", description = "Employer release funds cho freelancer khi milestone hoàn thành")
    public ResponseEntity<EscrowResponse> releaseFunds(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID milestoneId
    ) {
        UUID employerId = UUID.fromString(userDetails.getUsername());
        EscrowResponse response = escrowService.releaseFunds(milestoneId, employerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Refund funds về employer
     * POST /api/escrow/refund/{milestoneId}
     */
    @PostMapping("/refund/{milestoneId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund funds", description = "Admin refund funds về employer (khi dispute hoặc reject)")
    public ResponseEntity<EscrowResponse> refundFunds(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID milestoneId
    ) {
        UUID adminId = UUID.fromString(userDetails.getUsername());
        EscrowResponse response = escrowService.refundFunds(milestoneId, adminId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy escrow theo milestone
     * GET /api/escrow/milestone/{milestoneId}
     */
    @GetMapping("/milestone/{milestoneId}")
    @Operation(summary = "Lấy escrow theo milestone", description = "Xem thông tin escrow của một milestone")
    public ResponseEntity<EscrowResponse> getEscrowByMilestone(
            @PathVariable UUID milestoneId
    ) {
        EscrowResponse response = escrowService.getEscrowByMilestoneId(milestoneId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy tất cả escrow của project
     * GET /api/escrow/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    @Operation(summary = "Lấy escrow của project", description = "Xem tất cả escrow records của một project")
    public ResponseEntity<List<EscrowResponse>> getEscrowsByProject(
            @PathVariable UUID projectId
    ) {
        List<EscrowResponse> escrows = escrowService.getEscrowsByProject(projectId);
        return ResponseEntity.ok(escrows);
    }

    /**
     * Lấy tổng số tiền đang lock
     * GET /api/escrow/project/{projectId}/total-locked
     */
    @GetMapping("/project/{projectId}/total-locked")
    @Operation(summary = "Tổng số tiền đang lock", description = "Tính tổng số tiền đang được lock trong escrow của project")
    public ResponseEntity<BigDecimal> getTotalLockedAmount(
            @PathVariable UUID projectId
    ) {
        BigDecimal total = escrowService.getTotalLockedAmount(projectId);
        return ResponseEntity.ok(total);
    }

    /**
     * Lấy escrow statistics của employer
     * GET /api/escrow/statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Thống kê escrow", description = "Xem thống kê escrow của tôi (locked, released, refunded)")
    public ResponseEntity<EscrowService.EscrowStatistics> getMyEscrowStatistics(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        EscrowService.EscrowStatistics statistics = escrowService.getEmployerEscrowStatistics(userId);
        return ResponseEntity.ok(statistics);
    }
}
