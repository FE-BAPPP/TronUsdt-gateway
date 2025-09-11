package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.dto.SweepResultDto;
import com.UsdtWallet.UsdtWallet.model.entity.WalletTransaction;
import com.UsdtWallet.UsdtWallet.model.entity.TokenSweep;
import com.UsdtWallet.UsdtWallet.model.entity.GasTopup;
import com.UsdtWallet.UsdtWallet.model.entity.HdMasterWallet;
import com.UsdtWallet.UsdtWallet.repository.WalletTransactionRepository;
import com.UsdtWallet.UsdtWallet.repository.TokenSweepRepository;
import com.UsdtWallet.UsdtWallet.repository.GasTopupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsdtSweepService {

    private final TronApiService tronApiService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TokenSweepRepository tokenSweepRepository;
    private final GasTopupRepository gasTopupRepository; 
    private final HdWalletService hdWalletService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PointsService pointsService;
    private final NotificationService notificationService; 

    @Value("${sweep.min.amount:5}")
    private BigDecimal minimumSweepAmount;

    @Value("${sweep.gas.limit:15}")
    private BigDecimal gasLimitTrx;

    @Value("${sweep.batch.size:10}")
    private Integer sweepBatchSize;

    @Value("${sweep.enabled:true}")
    private Boolean sweepEnabled;

    @Value("${tron.usdt.contract:TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf}")
    private String usdtContractAddress;

    private static final String SWEEP_LOCK_KEY = "sweep:lock";
    private static final String SWEEP_STATS_KEY = "sweep:stats";

    /**
     * Scheduled sweep every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void scheduledSweep() {
        if (!sweepEnabled) {
            log.debug("USDT sweep is disabled");
            return;
        }

        try {
            // Acquire lock to prevent multiple sweep processes
            Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(SWEEP_LOCK_KEY, "locked", 600, TimeUnit.SECONDS); // 10 min lock

            if (!Boolean.TRUE.equals(lockAcquired)) {
                log.debug("Sweep already in progress, skipping...");
                return;
            }

            log.info("🧹 Starting scheduled USDT sweep...");

            SweepResultDto result = sweepUnsweptDeposits();

            log.info("✅ Scheduled sweep completed: {} transactions, {} USDT total",
                result.getTotalTransactions(), result.getTotalAmount());

            // Store sweep stats
            redisTemplate.opsForValue().set(SWEEP_STATS_KEY, result, 24, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("❌ Error during scheduled sweep", e);
        } finally {
            // Release lock
            redisTemplate.delete(SWEEP_LOCK_KEY);
        }
    }

    /**
     * Sweep all unswept deposits
     */
    @Transactional
    public SweepResultDto sweepUnsweptDeposits() {
        // Get master wallet info first
        HdMasterWallet masterWallet = hdWalletService.getMasterWallet();
        String masterAddress = masterWallet.getMasterAddress();

        SweepResultDto.SweepResultDtoBuilder resultBuilder = SweepResultDto.builder()
            .masterWalletAddress(masterAddress)
            .successfulSweeps(new ArrayList<>())
            .failedSweeps(new ArrayList<>())
            .totalAmount(BigDecimal.ZERO)
            .totalGasUsed(BigDecimal.ZERO)
            .totalTransactions(0);

        try {
            // Get unswept deposits
            List<WalletTransaction> unsweptDeposits = walletTransactionRepository.findUnsweptDeposits();

            if (unsweptDeposits.isEmpty()) {
                log.info("No unswept deposits found");
                return resultBuilder.status("SUCCESS").message("No deposits to sweep").build();
            }

            log.info("Found {} unswept deposits to process", unsweptDeposits.size());

            // Check master wallet TRX balance for gas
            BigDecimal masterTrxBalance = tronApiService.getTrxBalance(masterAddress);
            BigDecimal requiredGas = gasLimitTrx.multiply(new BigDecimal(Math.min(unsweptDeposits.size(), sweepBatchSize)));

            if (masterTrxBalance.compareTo(requiredGas) < 0) {
                String message = String.format("Insufficient TRX for gas. Required: %s, Available: %s",
                    requiredGas, masterTrxBalance);
                log.error("❌ " + message);
                return resultBuilder.status("FAILED").message(message).build();
            }

            // Process deposits in batches
            List<WalletTransaction> depositsToSweep = unsweptDeposits.stream()
                .filter(tx -> tx.getAmount().compareTo(minimumSweepAmount) >= 0)
                .limit(sweepBatchSize)
                .toList();

            int successCount = 0;
            BigDecimal totalSwept = BigDecimal.ZERO;
            BigDecimal totalGas = BigDecimal.ZERO;
            List<SweepResultDto.SweepTransactionDto> successfulSweeps = new ArrayList<>();
            List<SweepResultDto.SweepTransactionDto> failedSweeps = new ArrayList<>();

            for (WalletTransaction deposit : depositsToSweep) {
                try {
                    SweepResultDto.SweepTransactionDto sweepResult = sweepSingleDeposit(deposit);

                    if ("SUCCESS".equals(sweepResult.getStatus())) {
                        successfulSweeps.add(sweepResult);
                        successCount++;
                        totalSwept = totalSwept.add(sweepResult.getAmount());
                        totalGas = totalGas.add(sweepResult.getGasUsed());
                    } else {
                        failedSweeps.add(sweepResult);
                    }

                } catch (Exception e) {
                    log.error("Error sweeping deposit {}: {}", deposit.getTxHash(), e.getMessage());

                    SweepResultDto.SweepTransactionDto failedSweep = SweepResultDto.SweepTransactionDto.builder()
                        .fromAddress(deposit.getToAddress())
                        .amount(deposit.getAmount())
                        .status("FAILED")
                        .errorMessage(e.getMessage())
                        .build();

                    failedSweeps.add(failedSweep);
                }
            }

            String status = successCount > 0 ? "SUCCESS" : "FAILED";
            String message = String.format("Swept %d/%d deposits, Total: %s USDT",
                successCount, depositsToSweep.size(), totalSwept);

            return resultBuilder
                .status(status)
                .message(message)
                .totalTransactions(successCount)
                .totalAmount(totalSwept)
                .totalGasUsed(totalGas)
                .successfulSweeps(successfulSweeps)
                .failedSweeps(failedSweeps)
                .build();

        } catch (Exception e) {
            log.error("Error during sweep process", e);
            return resultBuilder
                .status("FAILED")
                .message("Sweep failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Sweep a single deposit
     */
    @Transactional
    public SweepResultDto.SweepTransactionDto sweepSingleDeposit(WalletTransaction deposit) {
        String fromAddress = deposit.getToAddress(); // Child wallet
        BigDecimal amount = deposit.getAmount();

        TokenSweep tokenSweep = new TokenSweep();
        tokenSweep.setChildIndex(hdWalletService.getChildIndexByAddress(fromAddress));
        tokenSweep.setChildAddress(fromAddress);
        tokenSweep.setMasterAddress(hdWalletService.getMasterWallet().getMasterAddress());
        tokenSweep.setAmount(amount);
        tokenSweep.setStatus(TokenSweep.SweepStatus.PENDING);
        tokenSweep.setRetryCount(0);

        tokenSweep = tokenSweepRepository.save(tokenSweep);
        log.info("Đã tạo TokenSweep record ID: {} cho việc sweep {} USDT từ {}",
            tokenSweep.getId(), amount, fromAddress);

        try {
            HdMasterWallet masterWallet = hdWalletService.getMasterWallet();
            String masterAddress = masterWallet.getMasterAddress();

            log.info("🧹 Sweeping {} USDT from {} to master wallet", amount, fromAddress);

            BigDecimal currentUsdtBalance = tronApiService.getUsdtBalance(fromAddress);
            if (currentUsdtBalance.compareTo(amount) < 0) {
                String error = String.format("Insufficient USDT balance. Expected: %s, Current: %s", amount, currentUsdtBalance);
                log.warn(error);

                // CẬP NHẬT TOKENSWEEP STATUS
                tokenSweep.setStatus(TokenSweep.SweepStatus.FAILED);
                tokenSweep.setErrorMessage(error);
                tokenSweepRepository.save(tokenSweep);

                return SweepResultDto.SweepTransactionDto.builder()
                    .fromAddress(fromAddress)
                    .amount(amount)
                    .status("FAILED")
                    .errorMessage(error)
                    .build();
            }

            BigDecimal trxBalance = tronApiService.getTrxBalance(fromAddress);
            log.debug("Child wallet TRX balance: {} TRX, Required gas: {} TRX", trxBalance, gasLimitTrx);

            if (trxBalance.compareTo(gasLimitTrx) < 0) {
                log.info("⛽ Child wallet needs TRX for gas. Current: {} TRX, Required: {} TRX",
                    trxBalance, gasLimitTrx);

                boolean gasSent = sendGasToChildWallet(fromAddress, gasLimitTrx);
                if (!gasSent) {
                    String error = "Failed to send TRX gas to child wallet";

                    tokenSweep.setStatus(TokenSweep.SweepStatus.FAILED);
                    tokenSweep.setErrorMessage(error);
                    tokenSweepRepository.save(tokenSweep);

                    return SweepResultDto.SweepTransactionDto.builder()
                        .fromAddress(fromAddress)
                        .amount(amount)
                        .status("FAILED")
                        .errorMessage(error)
                        .build();
                }

                // Wait for gas confirmation
                if (!waitForGasConfirmation(fromAddress, gasLimitTrx, 60)) {
                    String error = "Gas transaction not confirmed after 60 seconds";
                    tokenSweep.setStatus(TokenSweep.SweepStatus.FAILED);
                    tokenSweep.setErrorMessage(error);
                    tokenSweepRepository.save(tokenSweep);

                    return SweepResultDto.SweepTransactionDto.builder()
                        .fromAddress(fromAddress)
                        .amount(amount)
                        .status("FAILED")
                        .errorMessage(error)
                        .build();
                }
            }

            String childPrivateKey = hdWalletService.getPrivateKeyForAddress(fromAddress);
            String rawTransaction = tronApiService.createUsdtTransferTransaction(fromAddress, masterAddress, amount);
            if (rawTransaction == null) {
                throw new RuntimeException("Failed to create USDT transaction");
            }

            String signedTransaction = tronApiService.signTransaction(rawTransaction, childPrivateKey);
            if (signedTransaction == null) {
                throw new RuntimeException("Failed to sign USDT transaction");
            }

            
            log.info("📡 Broadcasting USDT transaction");
            String txHash = tronApiService.broadcastTransaction(signedTransaction);
            if (txHash == null) {
                throw new RuntimeException("Failed to broadcast USDT transaction");
            }

            tokenSweep.setSweepTxHash(txHash);
            tokenSweep.setStatus(TokenSweep.SweepStatus.SENT); // Đã broadcast, chờ confirm
            tokenSweepRepository.save(tokenSweep);

            // Update WalletTransaction
            deposit.setIsSwept(true);
            deposit.setSweepTxHash(txHash);
            deposit.setSweptAt(LocalDateTime.now());
            walletTransactionRepository.save(deposit);

            // Tạo sweep transaction record
            WalletTransaction sweepTx = WalletTransaction.builder()
                .txHash(txHash)
                .fromAddress(fromAddress)
                .toAddress(masterAddress)
                .amount(amount)
                .tokenAddress(usdtContractAddress)
                .transactionType(WalletTransaction.TransactionType.SWEEP)
                .direction(WalletTransaction.TransactionDirection.OUT)
                .status(WalletTransaction.TransactionStatus.PENDING)
                .userId(deposit.getUserId())
                .childWalletId(deposit.getChildWalletId())
                .gasUsed(gasLimitTrx)
                .build();

            walletTransactionRepository.save(sweepTx);

            log.info("✅ Sweep process completed: {} USDT from {} - TokenSweep ID: {} - TxHash: {}",
                amount, fromAddress, tokenSweep.getId(), txHash);

            return SweepResultDto.SweepTransactionDto.builder()
                .fromAddress(fromAddress)
                .txHash(txHash)
                .amount(amount)
                .status("SUCCESS")
                .gasUsed(gasLimitTrx)
                .build();

        } catch (Exception e) {
            log.error("❌ Error sweeping deposit {}: {}", deposit.getTxHash(), e.getMessage(), e);

            tokenSweep.setStatus(TokenSweep.SweepStatus.FAILED);
            tokenSweep.setErrorMessage(e.getMessage());
            tokenSweep.setRetryCount(tokenSweep.getRetryCount() + 1);
            tokenSweepRepository.save(tokenSweep);

            return SweepResultDto.SweepTransactionDto.builder()
                .fromAddress(deposit.getToAddress())
                .amount(deposit.getAmount())
                .status("FAILED")
                .errorMessage(e.getMessage())
                .build();
        }
    }

    /**
     * Send TRX for gas to child wallet
     */
    private boolean sendGasToChildWallet(String childAddress, BigDecimal gasAmount) {
        GasTopup gasTopup = null;

        try {
            log.info("⛽ Sending {} TRX gas to {}", gasAmount, childAddress);

            Integer childIndex = hdWalletService.getChildIndexByAddress(childAddress);
            gasTopup = new GasTopup();
            gasTopup.setChildIndex(childIndex);
            gasTopup.setAmountTrx(gasAmount);
            gasTopup.setStatus(GasTopup.TopupStatus.PENDING);
            gasTopup = gasTopupRepository.save(gasTopup);

            log.info("Đã tạo GasTopup record ID: {} cho việc nạp {} TRX vào child #{}",
                gasTopup.getId(), gasAmount, childIndex);

            HdMasterWallet masterWallet = hdWalletService.getMasterWallet();
            String masterAddress = masterWallet.getMasterAddress();
            String masterPrivateKey = hdWalletService.getMasterPrivateKey();

            String rawTransaction = tronApiService.createTrxTransferTransaction(masterAddress, childAddress, gasAmount);
            if (rawTransaction == null) {
                throw new RuntimeException("Failed to create TRX transaction");
            }

            String signedTransaction = tronApiService.signTransaction(rawTransaction, masterPrivateKey);
            if (signedTransaction == null) {
                throw new RuntimeException("Failed to sign TRX transaction");
            }

            String txHash = tronApiService.broadcastTransaction(signedTransaction);
            if (txHash != null) {
                gasTopup.setTxHash(txHash);
                gasTopup.setStatus(GasTopup.TopupStatus.SENT);
                gasTopupRepository.save(gasTopup);

                log.info("✅ Gas sent: {} TRX to {}, TX: {} - GasTopup ID: {}",
                    gasAmount, childAddress, txHash, gasTopup.getId());
                return true;
            } else {
                // Broadcast thất bại
                gasTopup.setStatus(GasTopup.TopupStatus.FAILED);
                gasTopup.setTxHash("BROADCAST_FAILED");
                gasTopupRepository.save(gasTopup);
            }

        } catch (Exception e) {
            log.error("Error sending gas to child wallet {}: {}", childAddress, e.getMessage());

            if (gasTopup != null) {
                gasTopup.setStatus(GasTopup.TopupStatus.FAILED);
                gasTopup.setTxHash("ERROR: " + e.getMessage());
                gasTopupRepository.save(gasTopup);
            }
        }

        return false;
    }

    /**
     * Wait for gas confirmation
     */
    private boolean waitForGasConfirmation(String address, BigDecimal requiredGas, int maxRetries) {
        try {
            int retries = 0;
            BigDecimal trxBalance;

            do {
                trxBalance = tronApiService.getTrxBalance(address);
                log.info("⏳ Waiting for gas confirmation... Attempt {}: TRX balance is {} TRX", retries + 1, trxBalance);

                if (trxBalance.compareTo(requiredGas) >= 0) {
                    return true;
                }

                Thread.sleep(5000);
                retries++;

            } while (retries < maxRetries);

        } catch (Exception e) {
            log.error("Error waiting for gas confirmation: {}", e.getMessage());
        }

        return false;
    }

    /**
     * SCHEDULED: Confirm pending TokenSweeps trong database
     */
    @Scheduled(fixedDelay = 45000) // Chạy mỗi 45 giây
    @Transactional
    public void confirmPendingTokenSweeps() {
        try {
            List<TokenSweep> sentSweeps = tokenSweepRepository.findByStatus(TokenSweep.SweepStatus.SENT);

            if (sentSweeps.isEmpty()) {
                return;
            }

            log.info("🔍 Kiểm tra {} SENT token sweeps để confirm", sentSweeps.size());

            for (TokenSweep sweep : sentSweeps) {
                try {
                    if (sweep.getSweepTxHash() != null && !sweep.getSweepTxHash().isEmpty()) {
                        log.debug("🔍 Checking confirmation for TokenSweep ID {} with txHash: {}",
                            sweep.getId(), sweep.getSweepTxHash());

                        var txInfo = tronApiService.getTransactionInfo(sweep.getSweepTxHash());

                        if (txInfo != null) {
                            log.debug("📋 Transaction info cho {}: {}", sweep.getSweepTxHash(), txInfo);

                            Object result = txInfo.get("result");
                            Object receipt = txInfo.get("receipt");

                            // Kiểm tra cả result và receipt
                            if ("SUCCESS".equals(result) || (receipt != null && "SUCCESS".equals(((Map<?, ?>)receipt).get("result")))) {
                                sweep.setStatus(TokenSweep.SweepStatus.CONFIRMED);
                                sweep.setUpdatedAt(LocalDateTime.now());
                                tokenSweepRepository.save(sweep);

                                log.info("✅ TokenSweep confirmed: {} USDT from {} (ID: {}) - TxHash: {}",
                                    sweep.getAmount(), sweep.getChildAddress(), sweep.getId(), sweep.getSweepTxHash());

                                updateCorrespondingWalletTransaction(sweep.getSweepTxHash());

                                // Credit points ONLY after sweep confirmed
                                try {
                                    var optDeposit = walletTransactionRepository.findBySweepTxHash(sweep.getSweepTxHash());
                                    if (optDeposit.isPresent()) {
                                        WalletTransaction deposit = optDeposit.get();
                                        if (deposit.getPointsCredited() == null) {
                                            boolean credited = pointsService.creditPointsForDeposit(
                                                deposit.getUserId(),
                                                deposit.getAmount(),
                                                String.valueOf(deposit.getId()),
                                                deposit.getAmount()
                                            );
                                            if (credited) {
                                                deposit.setPointsCredited(deposit.getAmount());
                                                deposit.setPointsCreditedAt(LocalDateTime.now());
                                                // Mark deposit fully completed after points credited
                                                deposit.setStatus(WalletTransaction.TransactionStatus.COMPLETED);
                                                walletTransactionRepository.save(deposit);

                                                //  Send notifications: deposit confirmed + balance update
                                                try {
                                                    notificationService.notifyDepositConfirmed(
                                                        deposit.getUserId(),
                                                        deposit.getTxHash(),
                                                        deposit.getAmount(),
                                                        deposit.getAmount()
                                                    );
                                                    
                                                
                                                    BigDecimal newBalance = pointsService.getCurrentBalance(deposit.getUserId());
                                                    notificationService.notifyBalanceUpdate(deposit.getUserId(), newBalance);
                                                    
                                                    log.debug(" Sent deposit confirmation notifications to user: {}", deposit.getUserId());
                                                } catch (Exception notifException) {
                                                    log.warn(" Failed to send deposit confirmation notification (not critical): ", notifException);
                                                }

                                                log.info("🎁 Points credited after sweep confirmation: user={}, amount={} points, depositId={}",
                                                    deposit.getUserId(), deposit.getAmount(), deposit.getId());
                                            } else {
                                                log.warn("Points already credited or failed for depositId={} (tx={})", deposit.getId(), deposit.getTxHash());
                                            }
                                        }
                                    } else {
                                        log.warn("No deposit transaction found for sweep txHash {} to credit points", sweep.getSweepTxHash());
                                    }
                                } catch (Exception ce) {
                                    log.error("Failed to credit points after sweep confirmation tx={} : {}", sweep.getSweepTxHash(), ce.getMessage(), ce);
                                }

                            } else if ("FAILED".equals(result) || (receipt != null && "FAILED".equals(((Map<?, ?>)receipt).get("result")))) {
                                sweep.setStatus(TokenSweep.SweepStatus.FAILED);
                                sweep.setErrorMessage("Transaction failed on blockchain - Result: " + result);
                                sweep.setUpdatedAt(LocalDateTime.now());
                                tokenSweepRepository.save(sweep);

                                log.warn("❌ TokenSweep failed on blockchain: {} (ID: {}) - Result: {}",
                                    sweep.getSweepTxHash(), sweep.getId(), result);

                            } else {
                                // Transaction vẫn đang pending
                                log.debug("⏳ TokenSweep {} vẫn đang pending - Result: {}",
                                    sweep.getSweepTxHash(), result);

                                // Check timeout - nếu quá 30 phút thì retry
                                if (sweep.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
                                    log.warn("⚠️ TokenSweep {} đã pending quá 30 phút, có thể cần retry",
                                        sweep.getSweepTxHash());
                                }
                            }
                        } else {
                            log.warn("⚠️ Không lấy được transaction info cho TokenSweep {}", sweep.getSweepTxHash());

                            // Check timeout - nếu quá 1 giờ và không có txInfo thì mark failed
                            if (sweep.getCreatedAt().isBefore(LocalDateTime.now().minusHours(1))) {
                                sweep.setStatus(TokenSweep.SweepStatus.FAILED);
                                sweep.setErrorMessage("Timeout: Không thể lấy transaction info sau 1 giờ");
                                sweep.setUpdatedAt(LocalDateTime.now());
                                tokenSweepRepository.save(sweep);

                                log.error("❌ TokenSweep {} timeout - Mark as FAILED", sweep.getSweepTxHash());
                            }
                        }
                    } else {
                        log.warn("⚠️ TokenSweep ID {} không có txHash", sweep.getId());
                    }
                } catch (Exception e) {
                    log.error("❌ Lỗi check confirmation cho TokenSweep {}: {}",
                        sweep.getId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("❌ Lỗi confirm pending token sweeps: {}", e.getMessage(), e);
        }
    }

    /**
     * Cập nhật WalletTransaction tương ứng khi TokenSweep confirmed
     */
    private void updateCorrespondingWalletTransaction(String txHash) {
        try {
            Optional<WalletTransaction> optionalTx = walletTransactionRepository.findByTxHash(txHash);

            if (optionalTx.isPresent()) {
                WalletTransaction tx = optionalTx.get();
                if (tx.getTransactionType() == WalletTransaction.TransactionType.SWEEP &&
                    tx.getStatus() == WalletTransaction.TransactionStatus.PENDING) {

                    tx.setStatus(WalletTransaction.TransactionStatus.CONFIRMED);
                    walletTransactionRepository.save(tx);

                    log.info("✅ Updated WalletTransaction {} status to CONFIRMED", txHash);
                }
            } else {
                log.warn("⚠️ Không tìm thấy WalletTransaction với txHash: {}", txHash);
            }
        } catch (Exception e) {
            log.error("❌ Lỗi update WalletTransaction {}: {}", txHash, e.getMessage());
        }
    }

    /**
     * Manual sweep for specific address
     */
    @Transactional
    public SweepResultDto sweepAddress(String address) {
        log.info("🧹 Manual sweep for address: {}", address);

        List<WalletTransaction> unsweptDeposits = walletTransactionRepository
            .findUnsweptDepositsByAddress(address);

        if (unsweptDeposits.isEmpty()) {
            return SweepResultDto.builder()
                .status("SUCCESS")
                .message("No unswept deposits found for address")
                .totalTransactions(0)
                .totalAmount(BigDecimal.ZERO)
                .build();
        }

        // Process each deposit
        List<SweepResultDto.SweepTransactionDto> successful = new ArrayList<>();
        List<SweepResultDto.SweepTransactionDto> failed = new ArrayList<>();

        for (WalletTransaction deposit : unsweptDeposits) {
            SweepResultDto.SweepTransactionDto result = sweepSingleDeposit(deposit);
            if ("SUCCESS".equals(result.getStatus())) {
                successful.add(result);
            } else {
                failed.add(result);
            }
        }

        BigDecimal totalSwept = successful.stream()
            .map(SweepResultDto.SweepTransactionDto::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        HdMasterWallet masterWallet = hdWalletService.getMasterWallet();
        String masterAddress = masterWallet.getMasterAddress();

        return SweepResultDto.builder()
            .masterWalletAddress(masterAddress)
            .totalTransactions(successful.size())
            .totalAmount(totalSwept)
            .successfulSweeps(successful)
            .failedSweeps(failed)
            .status(successful.size() > 0 ? "SUCCESS" : "FAILED")
            .message(String.format("Swept %d/%d deposits", successful.size(), unsweptDeposits.size()))
            .build();
    }

    /**
     * Get sweep statistics
     */
    public Map<String, Object> getSweepStats() {
        Object cachedStats = redisTemplate.opsForValue().get(SWEEP_STATS_KEY);

        // Get current unswept count
        List<WalletTransaction> unswept = walletTransactionRepository.findUnsweptDeposits();
        BigDecimal unsweptAmount = unswept.stream()
            .map(WalletTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
            "lastSweepResult", cachedStats != null ? cachedStats : "No recent sweep",
            "unsweptDeposits", unswept.size(),
            "unsweptAmount", unsweptAmount,
            "isSweeping", redisTemplate.hasKey(SWEEP_LOCK_KEY),
            "sweepEnabled", sweepEnabled
        );
    }

    /**
     * Trigger immediate sweep with points credit 
     */
    public void triggerSweepWithPointsCredit(WalletTransaction depositTransaction) {
        try {
            String address = depositTransaction.getToAddress();
            log.info("🚀 Triggering immediate sweep with points credit for: {}", address);

            // Acquire lock to prevent interference with scheduled sweep
            String sweepLockKey = "sweep:address:" + address;
            Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(sweepLockKey, "locked", 300, TimeUnit.SECONDS); // 5 min lock

            if (!Boolean.TRUE.equals(lockAcquired)) {
                log.debug("Sweep already in progress for address {}, skipping...", address);
                return;
            }

            try {
                // Sweep the specific deposit
                SweepResultDto.SweepTransactionDto result = sweepSingleDeposit(depositTransaction);

                if ("SUCCESS".equals(result.getStatus())) {
                    log.info("✅ Sweep transaction broadcasted for deposit {} (txHash={}). Waiting for confirmation before crediting points.",
                        depositTransaction.getId(), result.getTxHash());
                } else {
                    log.warn("⚠️ Immediate sweep failed for {}: {}", address, result.getErrorMessage());
                }

            } finally {

                redisTemplate.delete(sweepLockKey);
            }

        } catch (Exception e) {
            log.error("Error in immediate sweep + points credit for deposit {}: {}",
                depositTransaction.getTxHash(), e.getMessage(), e);
        }
    }

    /**
     * SCHEDULED: Confirm pending GasTopups trong database
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void confirmPendingGasTopups() {
        try {
            List<GasTopup> sentTopups = gasTopupRepository.findByStatus(GasTopup.TopupStatus.SENT);

            if (sentTopups.isEmpty()) {
                return;
            }

            log.info("🔍 Kiểm tra {} SENT gas topups để confirm", sentTopups.size());

            for (GasTopup topup : sentTopups) {
                try {
                    if (topup.getTxHash() != null && !topup.getTxHash().startsWith("ERROR") && !topup.getTxHash().equals("BROADCAST_FAILED")) {
                        log.debug("🔍 Checking confirmation for GasTopup ID {} with txHash: {}",
                            topup.getId(), topup.getTxHash());

                        var txInfo = tronApiService.getTransactionInfo(topup.getTxHash());

                        if (txInfo != null) {
                            log.debug("📋 Transaction info cho gas topup {}: {}", topup.getTxHash(), txInfo);

                            Object result = txInfo.get("result");
                            Object receipt = txInfo.get("receipt");
                            Object contractResult = txInfo.get("contractResult");

                            boolean isSuccess = false;

                            if (receipt != null) {
                                // Có receipt = transaction đã được process
                                log.debug("🔍 Analyzing contractResult: {}, type: {}", contractResult,
                                    contractResult != null ? contractResult.getClass().getSimpleName() : "null");

                                boolean hasErrors = false;
                                if (contractResult != null && contractResult instanceof java.util.List<?>) {
                                    java.util.List<?> contractList = (java.util.List<?>)contractResult;
                                    log.debug("📋 ContractResult is List with {} items: {}", contractList.size(), contractList);

                                    // Đối với TRX transfer, contractResult=[] hoặc contractResult=[null] hoặc contractResult=[""]
                                    // đều có nghĩa là SUCCESS
                                    if (contractList.isEmpty()) {
                                        hasErrors = false;
                                        log.debug("✅ ContractResult empty - SUCCESS");
                                    } else {
                                        // Check nếu chỉ chứa null hoặc empty string
                                        boolean hasRealErrors = contractList.stream()
                                            .anyMatch(item -> item != null && !item.toString().trim().isEmpty());
                                        hasErrors = hasRealErrors;

                                        if (hasRealErrors) {
                                            log.debug("❌ ContractResult has real errors: {}", contractList);
                                          } else {
                                            log.debug("✅ ContractResult only has null/empty items - SUCCESS");
                                          }
                                    }
                                } else if (contractResult != null) {
                                    String contractStr = contractResult.toString().trim();
                                    hasErrors = !contractStr.isEmpty() && !"null".equals(contractStr);
                                    log.debug("📋 ContractResult is not List: '{}' - hasErrors: {}", contractStr, hasErrors);
                                }

                                if (!hasErrors) {
                                    isSuccess = true;
                                    log.debug("✅ TRX transfer thành công - có receipt, không có contractResult errors");
                                } else {
                                    isSuccess = false;
                                    log.debug("❌ TRX transfer thất bại - có contractResult errors: {}", contractResult);
                                }
                            } else if ("SUCCESS".equals(result)) {
                                isSuccess = true;
                                log.debug("✅ Transaction thành công - result=SUCCESS");
                            } else {
                                log.debug("⏳ Transaction chưa có receipt hoặc result - vẫn pending");
                            }

                            if (isSuccess) {
                                topup.setStatus(GasTopup.TopupStatus.CONFIRMED);
                                gasTopupRepository.save(topup);

                                log.info("✅ GasTopup confirmed: {} TRX to child #{} (ID: {}) - TxHash: {}",
                                    topup.getAmountTrx(), topup.getChildIndex(), topup.getId(), topup.getTxHash());

                            } else if ("FAILED".equals(result)) {
                                topup.setStatus(GasTopup.TopupStatus.FAILED);
                                gasTopupRepository.save(topup);

                                log.warn("❌ GasTopup failed on blockchain: {} (ID: {}) - Result: FAILED",
                                    topup.getTxHash(), topup.getId());

                            } else {
                                log.debug("⏳ GasTopup {} vẫn đang pending - Receipt: {}, ContractResult: {}",
                                    topup.getTxHash(), receipt, contractResult);

                                // Check timeout - nếu quá 20 phút thì retry
                                if (topup.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(20))) {
                                    log.warn("⚠️ GasTopup {} đã pending quá 20 phút, có thể cần retry",
                                        topup.getTxHash());
                                }
                            }
                        } else {
                            log.warn("⚠️ Không lấy được transaction info cho GasTopup {}", topup.getTxHash());

                            // Check timeout - nếu quá 30 phút và không có txInfo thì mark failed
                            if (topup.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
                                topup.setStatus(GasTopup.TopupStatus.FAILED);
                                gasTopupRepository.save(topup);

                                log.error("❌ GasTopup {} timeout - Mark as FAILED", topup.getTxHash());
                            }
                        }
                    } else {
                        log.warn("⚠️ GasTopup ID {} có txHash không hợp lệ: {}", topup.getId(), topup.getTxHash());
                    }
                } catch (Exception e) {
                    log.error("❌ Lỗi check confirmation cho GasTopup {}: {}",
                        topup.getId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("❌ Lỗi confirm pending gas topups: {}", e.getMessage(), e);
        }
    }
}
