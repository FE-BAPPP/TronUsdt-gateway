package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.entity.*;
import com.UsdtWallet.UsdtWallet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service cung cấp thống kê cho Admin Dashboard
 * 
 * Bao gồm:
 * - Tổng quan hệ thống (users, projects, jobs)
 * - Thống kê tài chính (deposits, withdrawals, escrow)
 * - Hoạt động theo thời gian
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminStatisticsService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final ProposalRepository proposalRepository;
    private final MilestoneRepository milestoneRepository;
    private final EscrowRepository escrowRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WithdrawalTransactionRepository withdrawalTransactionRepository;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final DisputeRepository disputeRepository;
    private final ReviewRepository reviewRepository;

    /**
     * Lấy tổng quan hệ thống
     */
    public SystemOverview getSystemOverview() {
        // User statistics
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long totalEmployers = userRepository.countByRole("EMPLOYER");
        long totalFreelancers = userRepository.countByRole("FREELANCER");

        // Job statistics
        long totalJobs = jobRepository.count();
        long openJobs = jobRepository.countByStatus(Job.JobStatus.OPEN);
        long closedJobs = jobRepository.countByStatus(Job.JobStatus.CLOSED);

        // Project statistics
        long totalProjects = projectRepository.count();
        long activeProjects = projectRepository.countByStatus(Project.ProjectStatus.IN_PROGRESS);
        long completedProjects = projectRepository.countByStatus(Project.ProjectStatus.COMPLETED);
        long disputedProjects = projectRepository.countByStatus(Project.ProjectStatus.DISPUTED);

        // Proposal statistics
        long totalProposals = proposalRepository.count();
        long pendingProposals = proposalRepository.countByStatus(Proposal.ProposalStatus.PENDING);
        long acceptedProposals = proposalRepository.countByStatus(Proposal.ProposalStatus.ACCEPTED);

        return SystemOverview.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalEmployers(totalEmployers)
                .totalFreelancers(totalFreelancers)
                .totalJobs(totalJobs)
                .openJobs(openJobs)
                .closedJobs(closedJobs)
                .totalProjects(totalProjects)
                .activeProjects(activeProjects)
                .completedProjects(completedProjects)
                .disputedProjects(disputedProjects)
                .totalProposals(totalProposals)
                .pendingProposals(pendingProposals)
                .acceptedProposals(acceptedProposals)
                .build();
    }

    /**
     * Lấy thống kê tài chính
     */
    public FinancialStatistics getFinancialStatistics() {
        // Wallet transactions
        BigDecimal totalDeposits = walletTransactionRepository.sumAmountByTransactionType("DEPOSIT");
        long depositCount = walletTransactionRepository.countByTransactionType("DEPOSIT");

        // Withdrawals
        BigDecimal totalWithdrawals = withdrawalTransactionRepository.sumAllWithdrawals();
        BigDecimal pendingWithdrawals = withdrawalTransactionRepository.sumByStatus("PENDING");
        long withdrawalCount = withdrawalTransactionRepository.count();

        // Escrow
        BigDecimal totalEscrowLocked = escrowRepository.findAll().stream()
                .filter(e -> e.getStatus() == Escrow.EscrowStatus.LOCKED)
                .map(Escrow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEscrowReleased = escrowRepository.findAll().stream()
                .filter(e -> e.getStatus() == Escrow.EscrowStatus.RELEASED)
                .map(Escrow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Platform fees collected
        BigDecimal totalPlatformFees = escrowRepository.findAll().stream()
                .filter(e -> e.getStatus() == Escrow.EscrowStatus.RELEASED)
                .map(Escrow::getPlatformFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate system balance
        BigDecimal systemBalance = totalDeposits.subtract(totalWithdrawals);

        return FinancialStatistics.builder()
                .totalDeposits(totalDeposits != null ? totalDeposits : BigDecimal.ZERO)
                .depositCount(depositCount)
                .totalWithdrawals(totalWithdrawals != null ? totalWithdrawals : BigDecimal.ZERO)
                .withdrawalCount(withdrawalCount)
                .pendingWithdrawals(pendingWithdrawals != null ? pendingWithdrawals : BigDecimal.ZERO)
                .totalEscrowLocked(totalEscrowLocked)
                .totalEscrowReleased(totalEscrowReleased)
                .totalPlatformFees(totalPlatformFees)
                .systemBalance(systemBalance)
                .build();
    }

    /**
     * Lấy hoạt động trong khoảng thời gian
     */
    public ActivityStatistics getActivityStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        // New users
        long newUsers = userRepository.countByDateCreatedBetween(startDate, endDate);

        // New jobs
        long newJobs = jobRepository.countByCreatedAtBetween(startDate, endDate);

        // New projects
        long newProjects = projectRepository.countByCreatedAtBetween(startDate, endDate);

        // Completed projects
        long completedProjects = projectRepository.countByCompletedAtBetween(startDate, endDate);

        // New disputes
        long newDisputes = disputeRepository.countByCreatedAtBetween(startDate, endDate);

        // Transactions
        long depositTransactions = walletTransactionRepository.countByCreatedAtBetweenAndTransactionType(
                startDate, endDate, "DEPOSIT");
        long withdrawalTransactions = withdrawalTransactionRepository.countByCreatedAtBetween(startDate, endDate);

        return ActivityStatistics.builder()
                .startDate(startDate)
                .endDate(endDate)
                .newUsers(newUsers)
                .newJobs(newJobs)
                .newProjects(newProjects)
                .completedProjects(completedProjects)
                .newDisputes(newDisputes)
                .depositTransactions(depositTransactions)
                .withdrawalTransactions(withdrawalTransactions)
                .build();
    }

    /**
     * Lấy top freelancers
     */
    public Map<String, Object> getTopFreelancers(int limit) {
        // Implementation would involve querying freelancers by earnings/ratings
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Top freelancers feature - to be implemented");
        return result;
    }

    /**
     * Lấy top employers
     */
    public Map<String, Object> getTopEmployers(int limit) {
        // Implementation would involve querying employers by spending/jobs posted
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Top employers feature - to be implemented");
        return result;
    }

    /**
     * Dashboard summary - tất cả thống kê cần thiết
     */
    public DashboardSummary getDashboardSummary() {
        SystemOverview overview = getSystemOverview();
        FinancialStatistics financial = getFinancialStatistics();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last30Days = now.minusDays(30);
        ActivityStatistics recentActivity = getActivityStatistics(last30Days, now);

        return DashboardSummary.builder()
                .overview(overview)
                .financial(financial)
                .recentActivity(recentActivity)
                .lastUpdated(now)
                .build();
    }

    // ========== Inner Classes ==========

    @lombok.Data
    @lombok.Builder
    public static class SystemOverview {
        private long totalUsers;
        private long activeUsers;
        private long totalEmployers;
        private long totalFreelancers;
        private long totalJobs;
        private long openJobs;
        private long closedJobs;
        private long totalProjects;
        private long activeProjects;
        private long completedProjects;
        private long disputedProjects;
        private long totalProposals;
        private long pendingProposals;
        private long acceptedProposals;
    }

    @lombok.Data
    @lombok.Builder
    public static class FinancialStatistics {
        private BigDecimal totalDeposits;
        private long depositCount;
        private BigDecimal totalWithdrawals;
        private long withdrawalCount;
        private BigDecimal pendingWithdrawals;
        private BigDecimal totalEscrowLocked;
        private BigDecimal totalEscrowReleased;
        private BigDecimal totalPlatformFees;
        private BigDecimal systemBalance;
    }

    @lombok.Data
    @lombok.Builder
    public static class ActivityStatistics {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private long newUsers;
        private long newJobs;
        private long newProjects;
        private long completedProjects;
        private long newDisputes;
        private long depositTransactions;
        private long withdrawalTransactions;
    }

    @lombok.Data
    @lombok.Builder
    public static class DashboardSummary {
        private SystemOverview overview;
        private FinancialStatistics financial;
        private ActivityStatistics recentActivity;
        private LocalDateTime lastUpdated;
    }
}
