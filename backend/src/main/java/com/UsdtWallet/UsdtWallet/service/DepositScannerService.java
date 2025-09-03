package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.entity.ChildWalletPool;
import com.UsdtWallet.UsdtWallet.model.entity.WalletTransaction;
import com.UsdtWallet.UsdtWallet.repository.ChildWalletPoolRepository;
import com.UsdtWallet.UsdtWallet.repository.WalletTransactionRepository;
import com.UsdtWallet.UsdtWallet.util.TronAddressUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepositScannerService {

    private final TronApiService tronApiService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ChildWalletPoolRepository childWalletPoolRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    @Qualifier("customStringRedisTemplate")
    private final RedisTemplate<String, String> customStringRedisTemplate; // Updated bean name
    private final PointsService pointsService;
    private final UsdtSweepService usdtSweepService; // Add sweep service

    @Value("${deposit.scanner.confirmations.required:3}")
    private Integer requiredConfirmations;

    @Value("${deposit.scanner.block.batch.size:50}")
    private Integer blockBatchSize;

    @Value("${tron.usdt.contract:TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf}")
    private String usdtContractAddress;

    @Value("${deposit.scanner.min.amount:0.1}")
    private BigDecimal minimumDepositAmount; // Lower minimum for testnet

    private static final String LAST_SCANNED_BLOCK_KEY = "deposit:scanner:last_block";
    private static final String SCANNING_LOCK_KEY = "deposit:scanner:lock";

    /**
     * Scheduled task to scan for new deposits every 30 seconds
     */
    @Scheduled(fixedDelay = 30000) // 30 seconds
    public void scanForDeposits() {
        try {
            // Acquire lock to prevent multiple scanning processes
            Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(SCANNING_LOCK_KEY, "locked", 60, TimeUnit.SECONDS);

            if (!Boolean.TRUE.equals(lockAcquired)) {
                log.debug("Deposit scanning already in progress, skipping...");
                return;
            }

            log.info("🔍 Starting deposit scan...");

            Long currentBlock = tronApiService.getLatestBlockNumber();
            if (currentBlock == null) {
                log.error("❌ Failed to get latest block number");
                return;
            }

            Long lastScannedBlock = getLastScannedBlock();
            Long fromBlock = lastScannedBlock + 1;
            Long toBlock = Math.min(fromBlock + blockBatchSize - 1, currentBlock - requiredConfirmations);

            if (fromBlock > toBlock) {
                log.debug("No new blocks to scan. Current: {}, Last scanned: {}", currentBlock, lastScannedBlock);
                return;
            }

            log.info("📊 Scanning blocks {} to {} (current: {})", fromBlock, toBlock, currentBlock);

            int depositsFound = scanBlockRange(fromBlock, toBlock);

            // Update last scanned block
            setLastScannedBlock(toBlock);

            log.info("✅ Scan completed. Blocks: {} to {}, Deposits found: {}", fromBlock, toBlock, depositsFound);

        } catch (Exception e) {
            log.error("❌ Error during deposit scanning", e);
        } finally {
            // Release lock
            redisTemplate.delete(SCANNING_LOCK_KEY);
        }
    }

    /**
     * Scan a range of blocks for deposits - OPTIMIZED VERSION
     */
    @Transactional
    public int scanBlockRange(Long fromBlock, Long toBlock) {
        int totalDeposits = 0;

        try {
            // Get all active child wallet addresses using StringRedisTemplate
            List<String> childAddresses = customStringRedisTemplate.opsForSet()
                .members("child_wallet_addresses")
                .stream()
                .toList();

            if (childAddresses.isEmpty()) {
                log.warn("No child wallet addresses found in cache");
                // Fallback: load from database if Redis cache is empty
                childAddresses = childWalletPoolRepository.findAll()
                    .stream()
                    .map(wallet -> wallet.getAddress())
                    .toList();
                log.info("Loaded {} child addresses from database as fallback", childAddresses.size());
            }

            log.info("🔍 OPTIMIZED SCAN: {} wallets, blocks {}-{}",
                childAddresses.size(), fromBlock, toBlock);

            // USE OPTIMIZED BATCH SCANNING instead of individual address scanning
            List<Map<String, Object>> allTransactions = tronApiService
                .getTransactionsInRangeOptimized(childAddresses, fromBlock, toBlock);

            log.info("📊 Found {} total transactions in block range", allTransactions.size());

            // Process each transaction found
            for (Map<String, Object> txData : allTransactions) {
                try {
                    String toAddress = (String) txData.get("to");
                    // Convert hex to base58 if needed
                    String toAddressBase58 = toAddress.startsWith("0x") ?
                        TronAddressUtil.hexToBase58(toAddress) : toAddress;

                    if (processTransaction(toAddressBase58, txData)) {
                        totalDeposits++;
                    }
                } catch (Exception e) {
                    log.error("Error processing transaction: {}", txData, e);
                }
            }

        } catch (Exception e) {
            log.error("Error in optimized block range scanning {} to {}", fromBlock, toBlock, e);
        }

        return totalDeposits;
    }

    /**
     * Process a single transaction
     */
    @Transactional
    public boolean processTransaction(String toAddress, Map<String, Object> txData) {
        try {
            String txHash = (String) txData.get("transaction_id");

            // Skip if we already processed this transaction
            if (walletTransactionRepository.existsByTxHash(txHash)) {
                return false;
            }

            // Extract transaction details - API trả về hex format
            String fromAddressHex = (String) txData.get("from");
            String toAddressHex = (String) txData.get("to");

            // Convert hex addresses to Base58 format for matching with DB
            String fromAddressBase58 = fromAddressHex != null ? TronAddressUtil.hexToBase58(fromAddressHex) : null;
            String toAddressBase58 = toAddressHex != null ? TronAddressUtil.hexToBase58(toAddressHex) : null;

            // Check if this transaction is for one of our child wallets (using Base58 format)
            if (toAddressBase58 == null || !toAddressBase58.equals(toAddress)) {
                log.debug("Transaction not for our wallet. Expected: {}, Got: {}", toAddress, toAddressBase58);
                return false;
            }

            Object contractAddressObj = txData.get("token_info");
            String contractAddress = "";

            // Handle contract address extraction properly
            if (contractAddressObj instanceof Map) {
                Map<String, Object> tokenInfo = (Map<String, Object>) contractAddressObj;
                Object addressObj = tokenInfo.get("address");
                contractAddress = addressObj != null ? addressObj.toString() : "";
            } else if (contractAddressObj != null) {
                contractAddress = contractAddressObj.toString();
            }

            // Only process USDT transactions
            if (!usdtContractAddress.equalsIgnoreCase(contractAddress)) {
                log.debug("Not USDT transaction. Contract: {}, Expected: {}", contractAddress, usdtContractAddress);
                return false;
            }

            Object valueObj = txData.get("value");
            if (valueObj == null) {
                return false;
            }

            // Convert USDT amount (6 decimals)
            BigDecimal amount = new BigDecimal(valueObj.toString())
                .divide(new BigDecimal("1000000"));

            // Skip small amounts
            if (amount.compareTo(minimumDepositAmount) < 0) {
                log.debug("Skipping small deposit: {} USDT to {}", amount, toAddress);
                return false;
            }

            // Get user ID from child wallet (using Base58 address from DB)
            Optional<ChildWalletPool> childWallet = childWalletPoolRepository.findByAddress(toAddress);
            if (childWallet.isEmpty() || childWallet.get().getUserId() == null) {
                log.warn("Deposit to unassigned wallet: {} amount: {} USDT", toAddress, amount);
                return false;
            }

            String userId = String.valueOf(childWallet.get().getUserId());
            Long blockNumber = ((Number) txData.get("block_number")).longValue();
            Long blockTimestamp = ((Number) txData.get("block_timestamp")).longValue();

            // Convert timestamp to LocalDateTime
            LocalDateTime transactionTime = java.time.Instant.ofEpochMilli(blockTimestamp)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();

            // Create transaction record with PENDING status (NOT credit points yet)
            WalletTransaction transaction = WalletTransaction.builder()
                .txHash(txHash)
                .fromAddress(fromAddressBase58)
                .toAddress(toAddressBase58)
                .amount(amount)
                .tokenAddress(contractAddress)
                .blockNumber(blockNumber)
                .blockTimestamp(transactionTime)
                .transactionType(WalletTransaction.TransactionType.DEPOSIT)
                .status(WalletTransaction.TransactionStatus.PENDING) // PENDING until swept
                .direction(WalletTransaction.TransactionDirection.IN)
                .userId(userId)
                .confirmationCount(requiredConfirmations)
                .build();

            walletTransactionRepository.save(transaction);

            // Update child wallet first deposit time if needed
            if (childWallet.get().getFirstDepositAt() == null) {
                childWallet.get().setFirstDepositAt(LocalDateTime.now());
                childWalletPoolRepository.save(childWallet.get());
            }

            log.info("💰 New deposit detected: {} USDT from {} to {} (User: {}) - PENDING sweep",
                amount, fromAddressBase58, toAddressBase58, userId);

            // Trigger sweep immediately - points will be credited AFTER successful sweep
            usdtSweepService.triggerSweepWithPointsCredit(transaction);

            return true;

        } catch (Exception e) {
            log.error("Error processing transaction: {}", txData, e);
            return false;
        }
    }


    /**
     * Manual scan for specific address
     */
    public int scanAddressManually(String address, Long fromBlock, Long toBlock) {
        log.info("🔍 Manual scan for address: {} from block {} to {}", address, fromBlock, toBlock);

        try {
            List<Map<String, Object>> transactions = tronApiService
                .getTransactionsInRange(address, fromBlock, toBlock);

            int processed = 0;
            for (Map<String, Object> txData : transactions) {
                if (processTransaction(address, txData)) {
                    processed++;
                }
            }

            log.info("✅ Manual scan completed. Address: {}, Processed: {}", address, processed);
            return processed;

        } catch (Exception e) {
            log.error("Error in manual scan for address: " + address, e);
            return 0;
        }
    }

    /**
     * Get last scanned block from Redis
     */
    private Long getLastScannedBlock() {
        Object value = redisTemplate.opsForValue().get(LAST_SCANNED_BLOCK_KEY);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        // If no last block, start from CURRENT block to avoid rescanning old blocks
        Long currentBlock = tronApiService.getLatestBlockNumber();
        if (currentBlock != null) {
            // Start from current block minus a small buffer for safety
            Long startBlock = currentBlock - 10; // Only scan last 10 blocks for safety
            log.info("🔄 First time setup: Starting deposit scanner from block {} (current: {})",
                startBlock, currentBlock);
            return startBlock;
        }
        return 0L;
    }

    /**
     * Set last scanned block in Redis
     */
    private void setLastScannedBlock(Long blockNumber) {
        redisTemplate.opsForValue().set(LAST_SCANNED_BLOCK_KEY, blockNumber);
    }

    /**
     * Get scanning statistics
     */
    public Map<String, Object> getScanningStats() {
        Long lastScannedBlock = getLastScannedBlock();
        Long currentBlock = tronApiService.getLatestBlockNumber();

        return Map.of(
            "lastScannedBlock", lastScannedBlock,
            "currentBlock", currentBlock != null ? currentBlock : 0,
            "blocksBehind", currentBlock != null ? currentBlock - lastScannedBlock : 0,
            "isScanning", redisTemplate.hasKey(SCANNING_LOCK_KEY)
        );
    }

    /**
     * Reset scan position (useful for fresh deployment or testing)
     */
    public void resetScanPosition(Long newPosition) {
        log.info("🔄 Resetting scan position to block: {}", newPosition);
        setLastScannedBlock(newPosition);
        log.info("✅ Scan position reset completed. Next scan will start from block: {}", newPosition + 1);
    }

    /**
     * Reset to recent blocks (current - offset)
     */
    public void resetToRecentBlocks(int offsetBlocks) {
        Long currentBlock = tronApiService.getLatestBlockNumber();
        if (currentBlock != null) {
            Long newPosition = currentBlock - offsetBlocks;
            resetScanPosition(newPosition);
            log.info("🔄 Reset to recent blocks. Current: {}, Offset: {}, New position: {}",
                currentBlock, offsetBlocks, newPosition);
        } else {
            log.error("❌ Failed to reset to recent blocks - could not get current block");
        }
    }
}
