package com.UsdtWallet.UsdtWallet.controller;

import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import com.UsdtWallet.UsdtWallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import com.UsdtWallet.UsdtWallet.repository.WalletTransactionRepository;
import com.UsdtWallet.UsdtWallet.repository.WithdrawalTransactionRepository;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WithdrawalTransactionRepository withdrawalTransactionRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search) {

        try {
            Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("dateCreated").descending());
            Page<User> usersPage;
            if (search != null && !search.trim().isEmpty()) {
                usersPage = userRepository.searchByUsername(search.trim(), pageable);
            } else {
                usersPage = userRepository.findAll(pageable);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("users", usersPage.getContent());
            payload.put("page", usersPage.getNumber());
            payload.put("size", usersPage.getSize());
            payload.put("totalElements", usersPage.getTotalElements());
            payload.put("totalPages", usersPage.getTotalPages());

            return ResponseEntity.ok(ApiResponse.success(payload));
        } catch (Exception e) {
            log.error("Error listing users", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to list users: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUser(@PathVariable String userId) {
        try {
            UUID uid = UUID.fromString(userId);
            Optional<User> opt = userRepository.findById(uid);
            if (opt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("User not found")
                        .build());
            }
            User u = opt.get();
            Map<String, Object> payload = Map.of(
                    "id", u.getId().toString(),
                    "username", u.getUsername(),
                    "email", u.getEmail(),
                    "fullName", u.getFullName(),
                    "role", u.getRole(),
                    "status", u.getStatus(),
                    "isActive", u.isActive(),
                    "createdAt", u.getDateCreated()
            );
            return ResponseEntity.ok(ApiResponse.success(payload));
        } catch (Exception e) {
            log.error("Error getting user", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get user: " + e.getMessage())
                    .build());
        }
    }

    // GET /api/admin/users/{userId}/stats
    @GetMapping("/{userId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats(@PathVariable String userId) {
        try {
            UUID uid = UUID.fromString(userId);
            Map<String, Object> stats = new HashMap<>();
            // Deposits: total amount and count via WalletTransactionRepository
            java.math.BigDecimal totalDeposits = walletTransactionRepository.sumAmountByUserIdAndTransactionTypeAndStatus(uid, com.UsdtWallet.UsdtWallet.model.entity.WalletTransaction.TransactionType.DEPOSIT, com.UsdtWallet.UsdtWallet.model.entity.WalletTransaction.TransactionStatus.CONFIRMED);
            if (totalDeposits == null) totalDeposits = java.math.BigDecimal.ZERO;
            long depositCount = walletTransactionRepository.countByUserIdAndTransactionType(uid, com.UsdtWallet.UsdtWallet.model.entity.WalletTransaction.TransactionType.DEPOSIT);
            // Withdrawals: total amount and count via WithdrawalTransactionRepository
            java.math.BigDecimal totalWithdrawals = withdrawalTransactionRepository.sumAmountByUserId(uid);
            long withdrawalCount = withdrawalTransactionRepository.countByUserId(uid);

            stats.put("totalDeposits", totalDeposits);
            stats.put("depositCount", depositCount);
            stats.put("totalWithdrawals", totalWithdrawals);
            stats.put("withdrawalCount", withdrawalCount);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Error getting user stats", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get user stats: " + e.getMessage())
                    .build());
        }
    }

    // Full deposit history (paginated)
    @GetMapping("/{userId}/deposits")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserDeposits(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            UUID uid = UUID.fromString(userId);
            Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
            var pageRes = walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(uid, pageable);
            Map<String, Object> payload = new HashMap<>();
            payload.put("deposits", pageRes.getContent());
            payload.put("page", pageRes.getNumber());
            payload.put("size", pageRes.getSize());
            payload.put("totalElements", pageRes.getTotalElements());
            payload.put("totalPages", pageRes.getTotalPages());
            return ResponseEntity.ok(ApiResponse.success(payload));
        } catch (Exception e) {
            log.error("Error getting user deposits", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get user deposits: " + e.getMessage())
                    .build());
        }
    }

    // Full withdrawal history (paginated)
    @GetMapping("/{userId}/withdrawals")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserWithdrawals(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            UUID uid = UUID.fromString(userId);
            Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
            var pageRes = withdrawalTransactionRepository.findByUserIdOrderByCreatedAtDesc(uid, pageable);
            Map<String, Object> payload = new HashMap<>();
            payload.put("withdrawals", pageRes.getContent());
            payload.put("page", pageRes.getNumber());
            payload.put("size", pageRes.getSize());
            payload.put("totalElements", pageRes.getTotalElements());
            payload.put("totalPages", pageRes.getTotalPages());
            return ResponseEntity.ok(ApiResponse.success(payload));
        } catch (Exception e) {
            log.error("Error getting user withdrawals", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to get user withdrawals: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStatus(
            @PathVariable String userId,
            @RequestBody Map<String, Object> body) {
        try {
            UUID uid = UUID.fromString(userId);
            Optional<User> opt = userRepository.findById(uid);
            if (opt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("User not found")
                        .build());
            }
            User u = opt.get();
            Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : null;
            Boolean active = body.get("isActive") != null ? Boolean.parseBoolean(body.get("isActive").toString()) : null;

            if (status != null) u.setStatus(status);
            if (active != null) u.setActive(active);

            userRepository.save(u);

            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Updated")));
        } catch (Exception e) {
            log.error("Error updating user status", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to update status: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRole(
            @PathVariable String userId,
            @RequestBody Map<String, String> body) {
        try {
            UUID uid = UUID.fromString(userId);
            Optional<User> opt = userRepository.findById(uid);
            if (opt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("User not found")
                        .build());
            }
            User u = opt.get();
            String role = body.get("role");
            if (role == null) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Role is required")
                        .build());
            }
            try {
                User.Role r = User.Role.valueOf(role.toUpperCase());
                u.setRole(r);
                // Keep boolean flags in sync for backward compatibility
                u.setAdmin(r == User.Role.ADMIN);
                u.setUser(r == User.Role.USER);
                userRepository.save(u);
                return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Role updated")));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Invalid role")
                        .build());
            }
        } catch (Exception e) {
            log.error("Error updating user role", e);
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Failed to update role: " + e.getMessage())
                    .build());
        }
    }
}