package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.util.TronAddressUtil;
import com.UsdtWallet.UsdtWallet.util.TronKeys;
import com.UsdtWallet.UsdtWallet.util.TronTransactionSigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.util.encoders.Hex;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class TronApiService {

    private final RestTemplate restTemplate;

    @Value("${tron.api.url:https://nile.trongrid.io}")
    private String tronApiUrl;

    @Value("${tron.usdt.contract:TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf}")
    private String usdtContractAddress;

    @Value("${tron.api.key:}")
    private String apiKey;

    private static final String API_KEY_HEADER = "TRON-PRO-API-KEY";

    // Cache for block timestamps to avoid repeated API calls
    private final Map<Long, Long> blockTimestampCache = new ConcurrentHashMap<>();

    // Retry queue for failed broadcasts (simple in-memory implementation)
    private final Map<String, BroadcastRetryInfo> retryQueue = new ConcurrentHashMap<>();

    private static class BroadcastRetryInfo {
        public final String signedTx;
        public final long firstAttempt;
        public int retryCount;

        public BroadcastRetryInfo(String signedTx) {
            this.signedTx = signedTx;
            this.firstAttempt = System.currentTimeMillis();
            this.retryCount = 0;
        }
    }

    /**
     * Get latest block number from Nile testnet
     */
    public Long getLatestBlockNumber() {
        try {
            String url = tronApiUrl + "/wallet/getnowblock";

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> blockHeader = (Map<String, Object>) response.getBody().get("block_header");
                if (blockHeader != null) {
                    Map<String, Object> rawData = (Map<String, Object>) blockHeader.get("raw_data");
                    if (rawData != null) {
                        Object numberObj = rawData.get("number");
                        if (numberObj instanceof Number) {
                            Long blockNumber = ((Number) numberObj).longValue();
                            log.debug("Latest Nile testnet block: {}", blockNumber);
                            return blockNumber;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting latest block number from Nile testnet", e);
        }
        return null;
    }

    /**
     * Get USDT balance using TriggerConstantContract (prioritized) with getAccount fallback
     */
    public BigDecimal getUsdtBalance(String address) {
        try {
            log.debug("Getting USDT balance for address: {}", address);

            // Priority 1: Use TriggerConstantContract for most accurate balance
            BigDecimal constantContractBalance = getUsdtBalanceFromConstantContract(address);
            if (constantContractBalance.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("USDT balance from triggerConstantContract: {} USDT", constantContractBalance);
                return constantContractBalance;
            }

            // Priority 2: Fallback to getAccount (may have delay, use for cache only)
            log.debug("Falling back to getAccount for USDT balance (may be delayed)");
            BigDecimal accountBalance = getAccountTrc20Balance(address, usdtContractAddress);
            log.debug("USDT balance from getAccount (cached): {} USDT", accountBalance);

            return accountBalance;

        } catch (Exception e) {
            log.error("Error getting USDT balance for address: {} on Nile testnet", address, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Get USDT balance using TriggerConstantContract (most accurate)
     */
    private BigDecimal getUsdtBalanceFromConstantContract(String address) {
        try {
            // Use TriggerConstantContract to call balanceOf function
            Map<String, Object> request = new HashMap<>();
            request.put("owner_address", "TLsV52sRDL79HXGGm9yzwKibb6BeruhUzy"); // Any address for constant call
            request.put("contract_address", usdtContractAddress);
            request.put("function_selector", "balanceOf(address)");

            // Encode the address parameter (remove 0x prefix if present and pad to 64 chars)
            String hexAddress = TronAddressUtil.base58ToHex(address);
            if (hexAddress.startsWith("0x")) hexAddress = hexAddress.substring(2);
            if (hexAddress.startsWith("41")) hexAddress = hexAddress.substring(2);
            // Pad to 64 characters
            String paddedAddress = String.format("%64s", hexAddress).replace(' ', '0');
            request.put("parameter", paddedAddress);
            request.put("visible", true);

            String url = tronApiUrl + "/wallet/triggerconstantcontract";

            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object resultObj = response.getBody().get("constant_result");
                if (resultObj instanceof List && !((List<?>) resultObj).isEmpty()) {
                    List<String> constantResult = (List<String>) resultObj;
                    String balanceHex = constantResult.get(0);

                    if (balanceHex != null && !balanceHex.isEmpty()) {
                        // Convert hex to BigInteger then to BigDecimal
                        BigInteger balanceWei = new BigInteger(balanceHex, 16);
                        // USDT has 6 decimals on Tron
                        BigDecimal usdtBalance = new BigDecimal(balanceWei).divide(new BigDecimal("1000000"));
                        return usdtBalance;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error calling triggerConstantContract for USDT balance: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get TRC20 token balance using account info
     */
    private BigDecimal getAccountTrc20Balance(String address, String contractAddress) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("address", address);
            request.put("visible", true);

            String url = tronApiUrl + "/wallet/getaccount";

            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Check for TRC20 token balances
                Object trc20Obj = response.getBody().get("trc20");
                if (trc20Obj instanceof List) {
                    List<Map<String, Object>> trc20List = (List<Map<String, Object>>) trc20Obj;
                    for (Map<String, Object> token : trc20List) {
                        String tokenAddress = (String) token.get("contract_address");
                        if (contractAddress.equalsIgnoreCase(tokenAddress)) {
                            Object balanceObj = token.get("balance");
                            if (balanceObj instanceof String) {
                                // USDT has 6 decimals on Tron
                                BigInteger balanceWei = new BigInteger((String) balanceObj);
                                return new BigDecimal(balanceWei).divide(new BigDecimal("1000000"));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting TRC20 balance for {}", address, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get TRX balance from Nile testnet
     */
    public BigDecimal getTrxBalance(String address) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("address", address);
            request.put("visible", true);

            String url = tronApiUrl + "/wallet/getaccount";

            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object balanceObj = response.getBody().get("balance");
                if (balanceObj instanceof Number) {
                    // Convert from sun to TRX (1 TRX = 1,000,000 sun)
                    BigInteger balanceInSun = BigInteger.valueOf(((Number) balanceObj).longValue());
                    BigDecimal trxBalance = new BigDecimal(balanceInSun).divide(new BigDecimal("1000000"));
                    log.debug("TRX balance for {}: {} TRX", address, trxBalance);
                    return trxBalance;
                }
            }
        } catch (Exception e) {
            log.error("Error getting TRX balance for address: {} on Nile testnet", address, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get TRC20 transactions for address using accurate block timestamps
     */
    public List<Map<String, Object>> getTransactionsInRange(String address, Long fromBlock, Long toBlock) {
        try {
            // Get accurate timestamps from block headers instead of approximation
            long fromTimestamp = getBlockTimestampAccurate(fromBlock);
            long toTimestamp = getBlockTimestampAccurate(toBlock);

            String url = String.format(
                "%s/v1/accounts/%s/transactions/trc20?limit=200&min_timestamp=%d&max_timestamp=%d&contract_address=%s",
                tronApiUrl, address, fromTimestamp, toTimestamp, usdtContractAddress);

            log.debug("Scanning Nile testnet transactions: {} (blocks {}-{}, timestamps {}-{})",
                address, fromBlock, toBlock, fromTimestamp, toTimestamp);

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object dataObj = response.getBody().get("data");
                if (dataObj instanceof List) {
                    List<Map<String, Object>> transactions = (List<Map<String, Object>>) dataObj;

                    // Filter transactions by SUCCESS status to avoid failed transactions
                    List<Map<String, Object>> validTransactions = new ArrayList<>();
                    for (Map<String, Object> tx : transactions) {
                        String txId = (String) tx.get("transaction_id");
                        if (isTransactionSuccessful(txId)) {
                            validTransactions.add(tx);
                        } else {
                            log.debug("Filtered out failed transaction: {}", txId);
                        }
                    }

                    log.debug("Found {} valid TRC20 transactions for address {} (filtered {} failed)",
                        validTransactions.size(), address, transactions.size() - validTransactions.size());
                    return validTransactions;
                }
            }
        } catch (Exception e) {
            log.error("Error getting transactions for address: {} on Nile testnet", address, e);
        }
        return List.of();
    }

    /**
     * Get accurate block timestamp using /wallet/getblockbynum API
     */
    private long getBlockTimestampAccurate(Long blockNumber) {
        // Check cache first
        if (blockTimestampCache.containsKey(blockNumber)) {
            return blockTimestampCache.get(blockNumber);
        }

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("num", blockNumber);

            String url = tronApiUrl + "/wallet/getblockbynum";

            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> blockHeader = (Map<String, Object>) response.getBody().get("block_header");
                if (blockHeader != null) {
                    Map<String, Object> rawData = (Map<String, Object>) blockHeader.get("raw_data");
                    if (rawData != null) {
                        Object timestampObj = rawData.get("timestamp");
                        if (timestampObj instanceof Number) {
                            long timestamp = ((Number) timestampObj).longValue();
                            // Cache the result
                            blockTimestampCache.put(blockNumber, timestamp);
                            log.debug("Block {} accurate timestamp: {}", blockNumber, timestamp);
                            return timestamp;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get accurate timestamp for block {}, falling back to approximation: {}",
                blockNumber, e.getMessage());
        }

        // Fallback to approximation if API fails
        long approximateTimestamp = blockNumber * 3000;
        blockTimestampCache.put(blockNumber, approximateTimestamp);
        return approximateTimestamp;
    }

    /**
     * Check if transaction was successful using gettransactioninfobyid
     */
    private boolean isTransactionSuccessful(String txId) {
        try {
            Map<String, Object> txInfo = getTransactionInfo(txId);
            if (txInfo != null) {
                Object resultObj = txInfo.get("result");
                if (resultObj instanceof String) {
                    return "SUCCESS".equals(resultObj);
                }
                // If no result field, assume success (for older transactions)
                return !txInfo.containsKey("result") || txInfo.get("result") == null;
            }
        } catch (Exception e) {
            log.debug("Could not verify transaction status for {}: {}", txId, e.getMessage());
        }
        // If we can't verify, assume success to avoid missing valid deposits
        return true;
    }

    /**
     * Broadcast transaction with retry mechanism
     */
    public String broadcastTransaction(String signedTransactionJson) {
        String txHash = broadcastTransactionInternal(signedTransactionJson);

        if (txHash == null) {
            // Add to retry queue for later processing
            String retryId = "retry_" + System.currentTimeMillis();
            retryQueue.put(retryId, new BroadcastRetryInfo(signedTransactionJson));
            log.warn("Transaction broadcast failed, added to retry queue: {}", retryId);

            // Try immediate retry once
            log.info("Attempting immediate retry for failed broadcast...");
            txHash = broadcastTransactionInternal(signedTransactionJson);
            if (txHash != null) {
                retryQueue.remove(retryId);
                log.info("✅ Immediate retry successful: {}", txHash);
            }
        }

        return txHash;
    }

    /**
     * Internal broadcast method without retry logic
     */
    private String broadcastTransactionInternal(String signedTransactionJson) {
        try {
            log.debug("Broadcasting signed transaction: {}", signedTransactionJson.substring(0, Math.min(200, signedTransactionJson.length())) + "...");

            // Parse JSON to get the transaction object
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> transactionMap = mapper.readValue(signedTransactionJson, new TypeReference<Map<String, Object>>() {});

            String url = tronApiUrl + "/wallet/broadcasttransaction";

            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Send the transaction object directly
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(transactionMap, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Boolean result = (Boolean) response.getBody().get("result");
                if (Boolean.TRUE.equals(result)) {
                    String txHash = (String) response.getBody().get("txid");
                    log.info("✅ Transaction broadcasted to Nile testnet: {}", txHash);
                    return txHash;
                } else {
                    log.error("❌ Failed to broadcast transaction to Nile testnet: {}", response.getBody());
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting transaction to Nile testnet: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Process retry queue - should be called periodically
     */
    public void processRetryQueue() {
        if (retryQueue.isEmpty()) {
            return;
        }

        log.info("Processing {} items in broadcast retry queue", retryQueue.size());

        retryQueue.entrySet().removeIf(entry -> {
            String retryId = entry.getKey();
            BroadcastRetryInfo retryInfo = entry.getValue();

            // Skip if too old (24 hours)
            if (System.currentTimeMillis() - retryInfo.firstAttempt > 24 * 60 * 60 * 1000) {
                log.warn("Removing expired retry item: {}", retryId);
                return true;
            }

            // Skip if too many retries
            if (retryInfo.retryCount >= 5) {
                log.warn("Removing retry item after {} attempts: {}", retryInfo.retryCount, retryId);
                return true;
            }

            // Try broadcast again
            retryInfo.retryCount++;
            String txHash = broadcastTransactionInternal(retryInfo.signedTx);
            if (txHash != null) {
                log.info("✅ Retry broadcast successful after {} attempts: {}", retryInfo.retryCount, txHash);
                return true; // Remove from queue
            }

            log.warn("Retry broadcast failed (attempt {}): {}", retryInfo.retryCount, retryId);
            return false; // Keep in queue
        });
    }

    /**
     * Get transaction info by ID from Nile testnet
     */
    public Map<String, Object> getTransactionInfo(String txid) {
        String url = tronApiUrl + "/wallet/gettransactioninfobyid";
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = Map.of("value", txid);
        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(req, headers), Map.class);
        return res.getBody();
    }


    /**
     * Get transaction by hash from Nile testnet
     */
    public Map<String, Object> getTransactionByHash(String txHash) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("value", txHash);

            String url = tronApiUrl + "/wallet/gettransactionbyid";

            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("Retrieved transaction {} from Nile testnet", txHash);
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Error getting transaction by hash: {} from Nile testnet", txHash, e);
        }
        return null;
    }

    /**
     * Get Nile testnet network info
     */
    public Map<String, Object> getNetworkInfo() {
        try {
            String url = tronApiUrl + "/wallet/getnodeinfo";

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Error getting Nile testnet info", e);
        }
        return Map.of("network", "nile", "status", "unknown");
    }

    /**
     * Create HTTP headers with API key if available
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (!apiKey.isEmpty()) {
            headers.set(API_KEY_HEADER, apiKey);
            log.debug("Using TronGrid API key for Nile testnet");
        }
        return headers;
    }

    /**
     * Check if address is valid Tron address
     */
    public boolean isValidTronAddress(String address) {
        return address != null && address.length() == 34 && address.startsWith("T");
    }

    /**
     * Get faucet TRX for testing (Nile testnet only)
     */
    public boolean requestFaucetTrx(String address) {
        try {
            // Note: This is a hypothetical faucet endpoint
            // You would need to implement actual faucet logic or use external faucet
            log.info("🚰 Requesting faucet TRX for address: {} on Nile testnet", address);

            // For now, just log the request
            log.warn("Faucet functionality not implemented. Please use Nile testnet faucet manually.");
            return false;

        } catch (Exception e) {
            log.error("Error requesting faucet TRX", e);
            return false;
        }
    }

    /**
     * Get transactions in range - OPTIMIZED VERSION
     * Instead of scanning each address individually, scan blocks and filter
     */
    public List<Map<String, Object>> getTransactionsInRangeOptimized(
            List<String> addresses, Long fromBlock, Long toBlock) {

        List<Map<String, Object>> allTransactions = new ArrayList<>();

        try {
            // Convert addresses to Set for faster lookup
            Set<String> addressSet = new HashSet<>();
            for (String addr : addresses) {
                // Add both Base58 and Hex formats for comprehensive matching
                addressSet.add(addr);
                addressSet.add(TronAddressUtil.base58ToHex(addr));
            }

            log.debug("Optimized scan: {} addresses, blocks {}-{}",
                addresses.size(), fromBlock, toBlock);

            // Scan blocks in batches to find relevant transactions
            long batchSize = 10; // Scan 10 blocks at a time
            for (long startBlock = fromBlock; startBlock <= toBlock; startBlock += batchSize) {
                long endBlock = Math.min(startBlock + batchSize - 1, toBlock);

                List<Map<String, Object>> blockTransactions =
                    getTransactionsFromBlockRange(startBlock, endBlock, addressSet);
                allTransactions.addAll(blockTransactions);
            }

            log.debug("Found {} transactions in optimized scan", allTransactions.size());

        } catch (Exception e) {
            log.error("Error in optimized transaction scanning", e);
        }

        return allTransactions;
    }

    /**
     * Scan specific block range for transactions involving our addresses with improved filtering
     */
    private List<Map<String, Object>> getTransactionsFromBlockRange(
            long fromBlock, long toBlock, Set<String> targetAddresses) {

        List<Map<String, Object>> transactions = new ArrayList<>();

        try {
            // Use TronGrid's events API with block numbers instead of timestamps
            String url = String.format("%s/v1/contracts/%s/events?event_name=Transfer&min_block_number=%d&max_block_number=%d&limit=200",
                tronApiUrl, usdtContractAddress, fromBlock, toBlock);

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object dataObj = response.getBody().get("data");
                if (dataObj instanceof List) {
                    List<Map<String, Object>> events = (List<Map<String, Object>>) dataObj;

                    log.debug("TronGrid returned {} events for blocks {}-{}", events.size(), fromBlock, toBlock);

                    int skippedCount = 0;
                    for (Map<String, Object> event : events) {
                        Map<String, Object> result = (Map<String, Object>) event.get("result");
                        if (result != null) {
                            String toAddress = (String) result.get("to");
                            Long eventBlockNumber = ((Number) event.get("block_number")).longValue();

                            // CRITICAL FIX: Filter out events outside our requested block range
                            if (eventBlockNumber < fromBlock || eventBlockNumber > toBlock) {
                                skippedCount++;
                                // Only log every 10th skip to reduce spam
                                if (skippedCount % 10 == 1) {
                                    log.debug("Skipping {} events from outside range {}-{} (showing every 10th)",
                                        skippedCount, fromBlock, toBlock);
                                }
                                continue;
                            }

                            // Check if this transaction is for one of our addresses
                            if (targetAddresses.contains(toAddress)) {
                                String txId = (String) event.get("transaction_id");

                                // IMPROVED: Check transaction status to avoid failed transactions
                                if (!isTransactionSuccessful(txId)) {
                                    log.debug("Skipping failed transaction: {} in block {}", txId, eventBlockNumber);
                                    continue;
                                }

                                Map<String, Object> txData = new HashMap<>();
                                txData.put("transaction_id", txId);
                                txData.put("from", result.get("from"));
                                txData.put("to", toAddress);
                                txData.put("value", result.get("value"));
                                txData.put("block_number", eventBlockNumber);
                                txData.put("block_timestamp", event.get("block_timestamp"));
                                txData.put("token_info", Map.of("address", usdtContractAddress));

                                transactions.add(txData);
                                
                                log.debug("Found valid deposit: {} USDT to {} in block {} (SUCCESS verified)",
                                    new BigDecimal(result.get("value").toString()).divide(new BigDecimal("1000000")),
                                    toAddress, eventBlockNumber);
                            }
                        }
                    }

                    if (skippedCount > 0) {
                        log.debug("Total skipped {} events outside range {}-{}", skippedCount, fromBlock, toBlock);
                    }
                }
            } else {
                log.warn("TronGrid API returned error for blocks {}-{}: {}", 
                    fromBlock, toBlock, response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error scanning block range {}-{}: {}", fromBlock, toBlock, e.getMessage(), e);
        }

        log.debug("Returning {} valid transactions for blocks {}-{}", transactions.size(), fromBlock, toBlock);
        return transactions;
    }

    /**
     * Get approximate timestamp for a block number
     */
    private long getBlockTimestamp(long blockNumber) {
        // Tron blocks are ~3 seconds apart, estimate timestamp
        long currentTime = System.currentTimeMillis();
        long currentBlock = getLatestBlockNumber();
        long blockDiff = currentBlock - blockNumber;
        return currentTime - (blockDiff * 3000); // 3 seconds per block
    }

    /**
     * Create TRX transfer transaction using TronGrid API
     */
    public String createTrxTransferTransaction(String fromAddress, String toAddress, BigDecimal amount) {
        try {
            log.info("Creating TRX transfer: {} TRX from {} to {}", amount, fromAddress, toAddress);

            // Convert TRX to sun (1 TRX = 1,000,000 sun)
            BigInteger amountInSun = amount.multiply(new BigDecimal("1000000")).toBigInteger();

            Map<String, Object> request = new HashMap<>();
            request.put("owner_address", fromAddress);
            request.put("to_address", toAddress);
            request.put("amount", amountInSun.longValue());
            request.put("visible", true);

            String url = tronApiUrl + "/wallet/createtransaction";

            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object rawDataObj = response.getBody().get("raw_data");
                if (rawDataObj != null) {
                    // Transaction created successfully
                    log.debug("TRX transaction created successfully");

                    // Convert response to JSON string
                    ObjectMapper mapper = new ObjectMapper();
                    String transactionJson = mapper.writeValueAsString(response.getBody());

                    // CRITICAL: Verify transaction has raw_data_hex
                    Map<String, Object> txMap = response.getBody();
                    if (!txMap.containsKey("raw_data_hex")) {
                        log.error("❌ CRITICAL: TRX Transaction missing raw_data_hex!");
                        log.error("Transaction response: {}", transactionJson);
                        throw new RuntimeException("TronGrid did not return raw_data_hex for TRX transaction");
                    }

                    String rawDataHex = (String) txMap.get("raw_data_hex");
                    log.info("✅ TRX Transaction created with raw_data_hex: {}", rawDataHex.substring(0, Math.min(32, rawDataHex.length())) + "...");

                    return transactionJson;
                } else {
                    log.error("❌ No raw_data in TronGrid TRX response: {}", response.getBody());
                }
            } else {
                log.error("❌ TronGrid TRX API error: {} - {}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Error creating TRX transfer transaction", e);
        }
        return null;
    }

    /**
     * Calculate safe fee limit based on TRX balance
     */
    private long calculateSafeFeeLimit(String fromAddress) {
        try {
            BigDecimal trxBalance = getTrxBalance(fromAddress);
            // Use maximum 80% of TRX balance for fee, minimum 1 TRX, maximum 15 TRX
            BigDecimal maxFeeInTrx = trxBalance.multiply(new BigDecimal("0.8"));

            // Clamp between 1 and 15 TRX
            if (maxFeeInTrx.compareTo(BigDecimal.ONE) < 0) {
                maxFeeInTrx = BigDecimal.ONE;
            } else if (maxFeeInTrx.compareTo(new BigDecimal("15")) > 0) {
                maxFeeInTrx = new BigDecimal("15");
            }

            // Convert to sun (1 TRX = 1,000,000 sun)
            long feeLimit = maxFeeInTrx.multiply(new BigDecimal("1000000")).longValue();

            log.debug("Calculated safe fee limit: {} TRX ({} sun) based on balance {} TRX",
                maxFeeInTrx, feeLimit, trxBalance);

            return feeLimit;

        } catch (Exception e) {
            log.warn("Could not calculate safe fee limit, using default: {}", e.getMessage());
            return 8_000_000; // 8 TRX default
        }
    }

    /**
     * Create USDT transfer transaction using TronGrid API
     */
    public String createUsdtTransferTransaction(String fromAddress, String toAddress, BigDecimal amount) {
        try {
            log.info("Creating USDT transfer: {} USDT from {} to {}", amount, fromAddress, toAddress);

            // Check TRX balance before creating transaction
            BigDecimal trxBalance = getTrxBalance(fromAddress);
            if (trxBalance.compareTo(new BigDecimal("1")) < 0) {
                log.warn("⚠️ Low TRX balance ({} TRX) for USDT transfer from {}", trxBalance, fromAddress);
            }

            // Convert USDT to smallest unit (6 decimals)
            BigInteger amountInWei = amount.multiply(new BigDecimal("1000000")).toBigInteger();

            // Create TRC20 transfer function call
            String methodId = "a9059cbb";

            // Convert Base58 to hex
            String toAddressHex = TronAddressUtil.base58ToHex(toAddress);
            if (toAddressHex.startsWith("0x")) {
                toAddressHex = toAddressHex.substring(2);
            }

            // ABI only accepts last 20 bytes (40 hex chars)
            if (toAddressHex.length() == 42) {
                toAddressHex = toAddressHex.substring(2); // remove 2 byte prefix (41 or A0)
            }

            String paddedToAddress = String.format("%64s", toAddressHex).replace(' ', '0');

            // Encode amount (pad to 64 chars)
            String amountHex = amountInWei.toString(16);
            String paddedAmount = String.format("%64s", amountHex).replace(' ', '0');

            // Only parameters (without selector)
            String parameter = paddedToAddress + paddedAmount;

            // Calculate safe fee limit based on actual TRX balance
            long safeFeeLimit = calculateSafeFeeLimit(fromAddress);

            Map<String, Object> request = new HashMap<>();
            request.put("owner_address", fromAddress);
            request.put("contract_address", usdtContractAddress);
            request.put("function_selector", "transfer(address,uint256)");
            request.put("parameter", parameter);
            request.put("fee_limit", safeFeeLimit);
            request.put("call_value", 0);
            request.put("visible", true);

            String url = tronApiUrl + "/wallet/triggersmartcontract";

            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object transactionObj = response.getBody().get("transaction");
                if (transactionObj != null) {
                    log.debug("USDT transaction created successfully with fee limit: {} TRX",
                        safeFeeLimit / 1_000_000.0);

                    // Convert transaction object to JSON string
                    ObjectMapper mapper = new ObjectMapper();
                    String transactionJson = mapper.writeValueAsString(transactionObj);

                    // CRITICAL: Verify transaction has raw_data_hex
                    Map<String, Object> txMap = (Map<String, Object>) transactionObj;
                    if (!txMap.containsKey("raw_data_hex")) {
                        log.error("❌ CRITICAL: Transaction missing raw_data_hex!");
                        log.error("Transaction response: {}", transactionJson);
                        throw new RuntimeException("TronGrid did not return raw_data_hex - cannot sign transaction");
                    }

                    String rawDataHex = (String) txMap.get("raw_data_hex");
                    log.info("✅ Transaction created with raw_data_hex: {}", rawDataHex.substring(0, Math.min(32, rawDataHex.length())) + "...");

                    return transactionJson;
                } else {
                    log.error("❌ No transaction object in TronGrid response: {}", response.getBody());
                }
            } else {
                log.error("❌ TronGrid API error: {} - {}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Error creating USDT transfer transaction", e);
        }
        return null;
    }

    /**
     * Sign transaction with private key using Tron standard protocol
     */
    public String signTransaction(String rawTransactionJson, String privateKeyHex) {
        try {
            log.debug("Signing transaction with Tron standard signer");
            return TronTransactionSigner.signTransaction(rawTransactionJson, privateKeyHex);

        } catch (Exception e) {
            log.error("❌ Failed to sign transaction: {}", e.getMessage());
            throw new RuntimeException("Transaction signing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convert TRON address to hex format
     */
    public String addressToHex(String address) {
        try {
            // Remove T prefix and convert base58 to hex
            if (!address.startsWith("T")) {
                throw new IllegalArgumentException("Invalid TRON address format");
            }

            // Decode base58 address
            byte[] decoded = org.bitcoinj.core.Base58.decode(address);

            // Remove checksum (last 4 bytes) and convert to hex
            byte[] addressBytes = java.util.Arrays.copyOfRange(decoded, 0, decoded.length - 4);

            // Convert to hex and remove 0x41 prefix (first byte is 0x41 for TRON mainnet)
            String hex = org.bouncycastle.util.encoders.Hex.toHexString(addressBytes);
            if (hex.startsWith("41")) {
                hex = hex.substring(2); // Remove 41 prefix
            }

            // Pad to 40 characters (20 bytes) if needed
            while (hex.length() < 40) {
                hex = "0" + hex;
            }

            return "0x" + hex;

        } catch (Exception e) {
            log.error("Failed to convert address {} to hex", address, e);
            throw new RuntimeException("Address conversion failed: " + e.getMessage());
        }
    }
}
