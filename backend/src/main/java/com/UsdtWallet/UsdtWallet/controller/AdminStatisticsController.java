package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.service.AdminStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller cho Admin Statistics
 * Endpoint: /api/admin/statistics
 */
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Statistics", description = "API thống kê cho Admin Dashboard")
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    /**
     * Lấy dashboard summary - tổng hợp tất cả thống kê
     * GET /api/admin/statistics/dashboard
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard summary", description = "Lấy tổng hợp tất cả thống kê cho admin dashboard")
    public ResponseEntity<AdminStatisticsService.DashboardSummary> getDashboardSummary() {
        AdminStatisticsService.DashboardSummary summary = adminStatisticsService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Lấy system overview
     * GET /api/admin/statistics/overview
     */
    @GetMapping("/overview")
    @Operation(summary = "System overview", description = "Thống kê tổng quan hệ thống (users, jobs, projects)")
    public ResponseEntity<AdminStatisticsService.SystemOverview> getSystemOverview() {
        AdminStatisticsService.SystemOverview overview = adminStatisticsService.getSystemOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * Lấy financial statistics
     * GET /api/admin/statistics/financial
     */
    @GetMapping("/financial")
    @Operation(summary = "Financial statistics", description = "Thống kê tài chính (deposits, withdrawals, escrow, fees)")
    public ResponseEntity<AdminStatisticsService.FinancialStatistics> getFinancialStatistics() {
        AdminStatisticsService.FinancialStatistics financial = adminStatisticsService.getFinancialStatistics();
        return ResponseEntity.ok(financial);
    }

    /**
     * Lấy activity statistics trong khoảng thời gian
     * GET /api/admin/statistics/activity
     */
    @GetMapping("/activity")
    @Operation(summary = "Activity statistics", description = "Thống kê hoạt động trong khoảng thời gian")
    public ResponseEntity<AdminStatisticsService.ActivityStatistics> getActivityStatistics(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        // Default to last 30 days if not specified
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        AdminStatisticsService.ActivityStatistics activity = 
                adminStatisticsService.getActivityStatistics(startDate, endDate);
        return ResponseEntity.ok(activity);
    }

    /**
     * Lấy activity statistics last 7 days
     * GET /api/admin/statistics/activity/week
     */
    @GetMapping("/activity/week")
    @Operation(summary = "Last 7 days activity", description = "Thống kê hoạt động 7 ngày gần nhất")
    public ResponseEntity<AdminStatisticsService.ActivityStatistics> getWeeklyActivity() {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(7);
        
        AdminStatisticsService.ActivityStatistics activity = 
                adminStatisticsService.getActivityStatistics(startDate, endDate);
        return ResponseEntity.ok(activity);
    }

    /**
     * Lấy activity statistics last 30 days
     * GET /api/admin/statistics/activity/month
     */
    @GetMapping("/activity/month")
    @Operation(summary = "Last 30 days activity", description = "Thống kê hoạt động 30 ngày gần nhất")
    public ResponseEntity<AdminStatisticsService.ActivityStatistics> getMonthlyActivity() {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(30);
        
        AdminStatisticsService.ActivityStatistics activity = 
                adminStatisticsService.getActivityStatistics(startDate, endDate);
        return ResponseEntity.ok(activity);
    }

    /**
     * Lấy activity statistics last year
     * GET /api/admin/statistics/activity/year
     */
    @GetMapping("/activity/year")
    @Operation(summary = "Last year activity", description = "Thống kê hoạt động 1 năm gần nhất")
    public ResponseEntity<AdminStatisticsService.ActivityStatistics> getYearlyActivity() {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusYears(1);
        
        AdminStatisticsService.ActivityStatistics activity = 
                adminStatisticsService.getActivityStatistics(startDate, endDate);
        return ResponseEntity.ok(activity);
    }
}
