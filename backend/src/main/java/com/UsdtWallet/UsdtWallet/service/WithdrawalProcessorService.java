package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.entity.WithdrawalTransaction;
import com.UsdtWallet.UsdtWallet.model.entity.HdMasterWallet;
import com.UsdtWallet.UsdtWallet.repository.WithdrawalTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalProcessorService {

    private final WithdrawalTransactionRepository withdrawalRepository;
    private final HdWalletService hdWalletService;
    private final TronApiService tronApiService;
    private final AuditLogService auditLogService;
    private final PointsService pointsService;
    private final NotificationService notificationService; 

    @Value("${withdrawal.confirmations.required:20}")
    private Integer requiredConfirmations;

    /**
     * Process withdrawal transaction
     */
    @Transactional
    public void processWithdrawal(WithdrawalTransaction withdrawal) {
        try {
            log.info("Processing withdrawal: ID={}, Amount={}, ToAddress={}",
                    withdrawal.getId(), withdrawal.getAmount(), withdrawal.getToAddress());

            // Check master wallet balance and TRX gas as before
            HdMasterWallet masterWallet = hdWalletService.getMasterWallet();
            BigDecimal masterBalance = tronApiService.getUsdtBalance(masterWallet.getMasterAddress());
            if (masterBalance.compareTo(withdrawal.getNetAmount()) < 0) {
                throw new RuntimeException("Insufficient USDT balance in master wallet: " + masterBalance);
            }
            BigDecimal trxBalance = hdWalletService.getTrxBalance(masterWallet.getMasterAddress());
            if (trxBalance.compareTo(new BigDecimal("20")) < 0) {
                throw new RuntimeException("Insufficient TRX balance for gas fees: " + trxBalance);
            }

            // Create and broadcast transaction
            String txHash = createAndBroadcastTransaction(withdrawal, masterWallet);

            // Update status
            withdrawal.setStatus(WithdrawalTransaction.WithdrawalStatus.BROADCASTING);
            withdrawal.setTxHash(txHash);
            withdrawal.setProcessedAt(LocalDateTime.now());
            withdrawalRepository.save(withdrawal);

            // Send notification: withdrawal processing
            try {
                notificationService.notifyWithdrawalProcessing(
                    withdrawal.getUserId(),
                    withdrawal.getId().toString(),
                    txHash,
                    withdrawal.getAmount()
                );
                log.debug("Sent withdrawal processing notification to user: {}", withdrawal.getUserId());
            } catch (Exception notifException) {
                log.warn("Failed to send withdrawal processing notification (not critical): ", notifException);
            }

            auditLogService.logWithdrawal(withdrawal, "Transaction broadcasted successfully");
            log.info("Withdrawal transaction broadcasted: ID={}, TxHash={}", withdrawal.getId(), txHash);

            monitorTransactionConfirmation(withdrawal.getId());

        } catch (Exception e) {
            log.error("Error processing withdrawal: {}", withdrawal.getId(), e);
            withdrawal.setStatus(WithdrawalTransaction.WithdrawalStatus.FAILED);
            withdrawal.setFailureReason(e.getMessage());
            withdrawalRepository.save(withdrawal);

            // Unlock on failure
            try {
                pointsService.unlockPointsForWithdrawal(withdrawal.getUserId(), withdrawal.getId().toString());
            } catch (Exception ex) {
                log.error("Failed to unlock points for withdrawal {}", withdrawal.getId(), ex);
            }

            auditLogService.logWithdrawal(withdrawal, "Processing failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create and broadcast USDT transfer transaction
     */
    private String createAndBroadcastTransaction(WithdrawalTransaction withdrawal, HdMasterWallet masterWallet) {
        try {
            String privateKey = hdWalletService.getMasterPrivateKey();

            // create raw tx
            String rawTx = tronApiService.createUsdtTransferTransaction(
                    masterWallet.getMasterAddress(),
                    withdrawal.getToAddress(),
                    withdrawal.getNetAmount()
            );

            // sign tx
            String signedTx = tronApiService.signTransaction(rawTx, privateKey);

            // broadcast tx
            String txHash = tronApiService.broadcastTransaction(signedTx);

            if (txHash == null || txHash.isEmpty()) {
                throw new RuntimeException("Failed to broadcast transaction");
            }

            return txHash;

        } catch (Exception e) {
            log.error("Error creating/broadcasting withdrawal transaction", e);
            throw new RuntimeException("Transaction creation failed: " + e.getMessage());
        }
    }

    /**
     * Monitor transaction confirmation
     */
    private void monitorTransactionConfirmation(Long withdrawalId) {
        log.info("Started monitoring confirmation for withdrawal: {}", withdrawalId);
    }

    /**
     * Check and update withdrawal confirmations - scheduled job
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000)
    public void checkWithdrawalConfirmations() {
        try {
            List<WithdrawalTransaction> pendingWithdrawals = withdrawalRepository.findByStatusIn(
                    List.of(
                            WithdrawalTransaction.WithdrawalStatus.BROADCASTING,
                            WithdrawalTransaction.WithdrawalStatus.SENT
                    )
            );

            for (WithdrawalTransaction withdrawal : pendingWithdrawals) {
                updateWithdrawalConfirmations(withdrawal);
            }

        } catch (Exception e) {
            log.error("Error checking withdrawal confirmations", e);
        }
    }

    /**
     * Update withdrawal confirmation status
     */
    @Transactional
    public void updateWithdrawalConfirmations(WithdrawalTransaction withdrawal) {
        try {
            if (withdrawal.getTxHash() == null) return;

            Map<String, Object> txInfo = tronApiService.getTransactionInfo(withdrawal.getTxHash());

            if (txInfo == null) {
                log.warn("Transaction not found on blockchain yet: {}", withdrawal.getTxHash());
                return;
            }

            // Fix: cast an toàn qua Number thay vì ép thẳng Long
            Object blockNumObj = txInfo.get("blockNumber");
            if (blockNumObj == null) {
                log.warn("blockNumber not found in txInfo: {}", txInfo);
                return;
            }
            long blockNumber = ((Number) blockNumObj).longValue();

            long currentBlock = tronApiService.getLatestBlockNumber();
            int confirmations = (int) (currentBlock - blockNumber);

            withdrawal.setBlockNumber(blockNumber);
            withdrawal.setConfirmations(confirmations);

            if (withdrawal.getStatus() == WithdrawalTransaction.WithdrawalStatus.BROADCASTING) {
                withdrawal.setStatus(WithdrawalTransaction.WithdrawalStatus.SENT);
            }

            if (confirmations >= requiredConfirmations) {
                withdrawal.setStatus(WithdrawalTransaction.WithdrawalStatus.CONFIRMED);
                withdrawal.setConfirmedAt(LocalDateTime.now());

                // Finalize deduction now
                try {
                    pointsService.finalizeWithdrawalDebit(
                        withdrawal.getUserId(),
                        withdrawal.getAmount(),
                        withdrawal.getId().toString()
                    );
                } catch (Exception e) {
                    log.error("Failed to finalize points for withdrawal {}", withdrawal.getId(), e);
                    // keep status but log critical
                }

                // Send notifications: withdrawal completed + balance update
                try {
                    notificationService.notifyWithdrawalCompleted(
                        withdrawal.getUserId(),
                        withdrawal.getTxHash(),
                        withdrawal.getAmount()
                    );
                    
                    // Send balance update notification
                    BigDecimal newBalance = pointsService.getCurrentBalance(withdrawal.getUserId());
                    notificationService.notifyBalanceUpdate(withdrawal.getUserId(), newBalance);
                    
                    log.debug(" Sent withdrawal completion notifications to user: {}", withdrawal.getUserId());
                } catch (Exception notifException) {
                    log.warn(" Failed to send withdrawal completion notification (not critical): ", notifException);
                }

                auditLogService.logWithdrawal(withdrawal,
                        "Transaction confirmed with " + confirmations + " confirmations");

                log.info("Withdrawal confirmed: ID={}, TxHash={}, Confirmations={}",
                        withdrawal.getId(), withdrawal.getTxHash(), confirmations);
            }

            withdrawalRepository.save(withdrawal);

        } catch (Exception e) {
            log.error("Error updating withdrawal confirmations: {}", withdrawal.getId(), e);
        }
    }

    /**
     * Get withdrawal processing statistics
     */
    public Map<String, Object> getProcessingStats() {
        return Map.of(
                "pending", withdrawalRepository.countByStatus(WithdrawalTransaction.WithdrawalStatus.PENDING),
                "processing", withdrawalRepository.countByStatus(WithdrawalTransaction.WithdrawalStatus.PROCESSING),
                "broadcasting", withdrawalRepository.countByStatus(WithdrawalTransaction.WithdrawalStatus.BROADCASTING),
                "sent", withdrawalRepository.countByStatus(WithdrawalTransaction.WithdrawalStatus.SENT),
                "confirmed", withdrawalRepository.countByStatus(WithdrawalTransaction.WithdrawalStatus.CONFIRMED),
                "failed", withdrawalRepository.countByStatus(WithdrawalTransaction.WithdrawalStatus.FAILED),
                "cancelled", withdrawalRepository.countByStatus(WithdrawalTransaction.WithdrawalStatus.CANCELLED)
        );
    }
}
