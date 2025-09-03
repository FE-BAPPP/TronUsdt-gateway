package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.entity.AuditLog;
import com.UsdtWallet.UsdtWallet.model.entity.WithdrawalTransaction;
import com.UsdtWallet.UsdtWallet.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log withdrawal activity
     */
    public void logWithdrawal(WithdrawalTransaction withdrawal, String action) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .userId(withdrawal.getUserId())
                .action("WITHDRAWAL_" + action.toUpperCase().replace(" ", "_"))
                .entityType("WITHDRAWAL")
                .entityId(withdrawal.getId().toString())
                .details(String.format("Amount: %s USDT, To: %s, Status: %s, TxHash: %s",
                    withdrawal.getAmount(),
                    withdrawal.getToAddress(),
                    withdrawal.getStatus(),
                    withdrawal.getTxHash() != null ? withdrawal.getTxHash() : "N/A"))
                .ipAddress("SYSTEM")
                .userAgent("SYSTEM")
                .timestamp(LocalDateTime.now())
                .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created for withdrawal: {}", withdrawal.getId());

        } catch (Exception e) {
            log.error("Failed to create audit log for withdrawal: {}", withdrawal.getId(), e);
        }
    }

    /**
     * Log user activity
     */
    public void logUserActivity(UUID userId, String action, String details, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType("USER")
                .entityId(userId.toString())
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created for user activity: {}", action);

        } catch (Exception e) {
            log.error("Failed to create audit log for user activity: {}", action, e);
        }
    }

    /**
     * Log admin activity
     */
    public void logAdminActivity(UUID adminId, String action, String details, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .userId(adminId)
                .action("ADMIN_" + action.toUpperCase().replace(" ", "_"))
                .entityType("ADMIN")
                .entityId(adminId.toString())
                .details(details)
                .ipAddress(ipAddress)
                .userAgent("ADMIN")
                .timestamp(LocalDateTime.now())
                .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created for admin activity: {}", action);

        } catch (Exception e) {
            log.error("Failed to create audit log for admin activity: {}", action, e);
        }
    }

    /**
     * Log security event
     */
    public void logSecurityEvent(String event, String details, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .action("SECURITY_" + event.toUpperCase().replace(" ", "_"))
                .entityType("SECURITY")
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .build();

            auditLogRepository.save(auditLog);
            log.warn("Security event logged: {}", event);

        } catch (Exception e) {
            log.error("Failed to create security audit log: {}", event, e);
        }
    }

    /**
     * Log system event
     */
    public void logSystemEvent(String event, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .action("SYSTEM_" + event.toUpperCase().replace(" ", "_"))
                .entityType("SYSTEM")
                .details(details)
                .ipAddress("SYSTEM")
                .userAgent("SYSTEM")
                .timestamp(LocalDateTime.now())
                .build();

            auditLogRepository.save(auditLog);
            log.debug("System event logged: {}", event);

        } catch (Exception e) {
            log.error("Failed to create system audit log: {}", event, e);
        }
    }
}
