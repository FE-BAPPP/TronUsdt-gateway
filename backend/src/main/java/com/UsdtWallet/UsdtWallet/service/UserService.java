package com.UsdtWallet.UsdtWallet.service;

import com.UsdtWallet.UsdtWallet.model.dto.request.UserRegistrationRequest;
import com.UsdtWallet.UsdtWallet.model.dto.response.UserRegistrationResponse;
import com.UsdtWallet.UsdtWallet.model.entity.ChildWalletPool;
import com.UsdtWallet.UsdtWallet.model.entity.User;
import com.UsdtWallet.UsdtWallet.repository.UserRepository;
import com.UsdtWallet.UsdtWallet.repository.ChildWalletPoolRepository;
import com.UsdtWallet.UsdtWallet.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final HdWalletService hdWalletService;
    private final PasswordEncoder passwordEncoder;
    private final ChildWalletPoolRepository childWalletPoolRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService; // added

    @Value("${security.passwordReset.withdrawalLockHours:24}")
    private long withdrawalLockHours;

    /**
     * Register new user with auto wallet assignment
     */
    @Transactional
    public UserRegistrationResponse registerUser(UserRegistrationRequest request) {
        // Validate input
        validateRegistrationRequest(request);

        // Check if user already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = createUser(request);
        User savedUser = userRepository.save(user);

        log.info("User created successfully: {}", savedUser.getUsername());

        // Auto assign wallet to user
        ChildWalletPool assignedWallet = hdWalletService.assignWalletToUser(savedUser.getId());

        // Update user's address field with wallet address
        savedUser.setAddress(assignedWallet.getAddress());
        savedUser = userRepository.save(savedUser);

        log.info("Wallet {} assigned to user {} and saved to user.address field",
            assignedWallet.getAddress(), savedUser.getUsername());

        return UserRegistrationResponse.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .walletAddress(assignedWallet.getAddress())
                .registeredAt(savedUser.getDateCreated())
                .message("User registered successfully with auto-assigned wallet")
                .build();
    }

    /**
     * Validate registration request
     */
    private void validateRegistrationRequest(UserRegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Password and confirm password do not match");
        }
    }

    /**
     * Create user entity from request
     */
    private User createUser(UserRegistrationRequest request) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(User.Role.USER)
                .status(1)
                .isActive(true)
                .isUser(true)
                .userCreated("SYSTEM")
                .build();
    }

    /**
     * User login
     */
    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is not active");
        }

        // Generate JWT token
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getRole().name());

        // Get user wallet address
        String walletAddress = getUserWalletAddress(user.getId());

        return Map.of(
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "role", user.getRole(),
                "walletAddress", walletAddress != null ? walletAddress : ""
            )
        );
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    /**
     * Get user by email
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /**
     * Get user wallet address
     */
    public String getUserWalletAddress(UUID userId) {
        // First check user.address field
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getAddress() != null && !user.getAddress().isEmpty()) {
            return user.getAddress();
        }

        ChildWalletPool wallet = childWalletPoolRepository.findByUserId(userId).orElse(null);
        return wallet != null ? wallet.getAddress() : null;
    }

    /**
     * Get comprehensive user wallet info
     */
    public Map<String, Object> getUserWalletInfo(UUID userId) {
        String walletAddress = getUserWalletAddress(userId);
        if (walletAddress == null) {
            throw new RuntimeException("No wallet assigned to user");
        }

        // Get wallet status from pool
        ChildWalletPool wallet = childWalletPoolRepository.findByAddress(walletAddress).orElse(null);

        return Map.of(
            "userId", userId.toString(),
            "walletAddress", walletAddress,
            "network", "TRC20",
            "status", wallet != null ? wallet.getStatus().toString() : "UNKNOWN",
            "derivationIndex", wallet != null ? wallet.getDerivationIndex() : -1,
            "assignedAt", wallet != null && wallet.getCreatedAt() != null ? wallet.getCreatedAt() : LocalDateTime.now(),
            "note", "Only send USDT (TRC20) to this address"
        );
    }

    /**
     * Get user transaction history
     */
    public Map<String, Object> getUserTransactions(UUID userId, int page, int size) {
        // This would integrate with WalletTransactionRepository to get user's deposit/withdrawal history
        String walletAddress = getUserWalletAddress(userId);

        return Map.of(
            "transactions", java.util.List.of(), // Placeholder - would get from repository
            "walletAddress", walletAddress,
            "totalElements", 0,
            "totalPages", 0,
            "currentPage", page,
            "pageSize", size
        );
    }

    /**
     * Get user info by ID
     */
    public Map<String, Object> getUserInfo(String userId) {
        UUID userUuid = UUID.fromString(userId);
        User user = userRepository.findById(userUuid)
            .orElseThrow(() -> new RuntimeException("User not found"));

        String walletAddress = getUserWalletAddress(userUuid);

        // Create HashMap
        Map<String, Object> userInfo = new java.util.HashMap<>();
        userInfo.put("id", user.getId().toString());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        userInfo.put("phone", user.getPhone() != null ? user.getPhone() : "");
        userInfo.put("role", user.getRole());
        userInfo.put("status", user.getStatus());
        userInfo.put("isActive", user.isActive());
        userInfo.put("walletAddress", walletAddress != null ? walletAddress : "");
        userInfo.put("createdAt", user.getDateCreated());
        userInfo.put("updatedAt", user.getDateUpdated());

        return userInfo;
    }

    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Create admin account with master wallet address
     */
    @Transactional
    public Map<String, Object> createAdminAccount(String username, String password, String email, String fullName) {
        try {
            log.info("=== CREATING ADMIN ACCOUNT ===");
            log.info("Username: {}, Email: {}, FullName: {}", username, email, fullName);

            // Check dependencies first
            log.info("Checking dependencies...");
            if (userRepository == null) {
                log.error("userRepository is null!");
                throw new RuntimeException("UserRepository is null");
            }
            if (passwordEncoder == null) {
                log.error("passwordEncoder is null!");
                throw new RuntimeException("PasswordEncoder is null");
            }
            if (hdWalletService == null) {
                log.error("hdWalletService is null!");
                throw new RuntimeException("HdWalletService is null");
            }
            log.info("All dependencies are OK");

            // Check if admin already exists
            log.info("Checking if username exists...");
            boolean usernameExists = userRepository.existsByUsername(username);
            log.info("Username exists check result: {}", usernameExists);

            if (usernameExists) {
                log.error("Admin username already exists: {}", username);
                throw new RuntimeException("Admin username already exists: " + username);
            }

            log.info("Checking if email exists...");
            boolean emailExists = userRepository.existsByEmail(email);
            log.info("Email exists check result: {}", emailExists);

            if (emailExists) {
                log.error("Admin email already exists: {}", email);
                throw new RuntimeException("Admin email already exists: " + email);
            }

            // Get master wallet address
            String masterWalletAddress = null;
            try {
                log.info("Getting master wallet...");
                var masterWallet = hdWalletService.getMasterWallet();
                if (masterWallet != null) {
                    masterWalletAddress = masterWallet.getMasterAddress();
                    log.info("Master wallet address: {}", masterWalletAddress);
                } else {
                    log.warn("Master wallet is null");
                }
            } catch (Exception e) {
                log.warn("Could not get master wallet address: {}", e.getMessage());
                log.warn("Will create admin without master wallet address");
            }

            // Create admin user
            log.info("Creating admin user entity...");
            log.info("Encoding password...");
            String encodedPassword = passwordEncoder.encode(password);
            log.info("Password encoded successfully, length: {}", encodedPassword.length());

            User admin = User.builder()
                    .username(username)
                    .email(email)
                    .password(encodedPassword)
                    .fullName(fullName)
                    .role(User.Role.ADMIN)
                    .status(1) // Active status
                    .isActive(true)
                    .isAdmin(true)
                    .isUser(false) // Admin is not regular user
                    .address(masterWalletAddress) // Set master wallet as admin address
                    .userCreated("SYSTEM")
                    .build();

            log.info("Admin user entity created successfully");
            log.info("Admin details - Username: {}, Email: {}, Role: {}",
                admin.getUsername(), admin.getEmail(), admin.getRole());

            log.info("Saving admin user to database...");
            User savedAdmin = userRepository.save(admin);
            log.info("Admin saved with ID: {}", savedAdmin.getId());

            log.info("Building result map...");
            Map<String, Object> adminData = Map.of(
                "id", savedAdmin.getId().toString(),
                "username", savedAdmin.getUsername(),
                "email", savedAdmin.getEmail(),
                "fullName", savedAdmin.getFullName() != null ? savedAdmin.getFullName() : "",
                "role", savedAdmin.getRole().toString(),
                "masterWalletAddress", masterWalletAddress != null ? masterWalletAddress : "",
                "createdAt", savedAdmin.getDateCreated() != null ? savedAdmin.getDateCreated().toString() : ""
            );

            Map<String, Object> result = Map.of(
                "success", true,
                "message", "Admin account created successfully",
                "admin", adminData
            );

            log.info("Admin account created successfully: {} with master wallet address: {}",
                    savedAdmin.getUsername(), masterWalletAddress);

            return result;

        } catch (Exception e) {
            log.error("Error in createAdminAccount - Exception type: {}", e.getClass().getSimpleName());
            log.error("Error in createAdminAccount - Message: {}", e.getMessage());
            log.error("Error in createAdminAccount - Stack trace: ", e);
            throw e; // Re-throw to let controller handle
        }
    }

    /**
     * Update password (expects already-encoded password)
     */
    @Transactional
    public boolean updatePassword(UUID userId, String encodedPassword) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        user.setPassword(encodedPassword);
        // Binance-like: mark password change time and disable withdrawals for N hours
        user.setPasswordChangedAt(java.time.LocalDateTime.now());
        user.setWithdrawalsDisabledUntil(java.time.LocalDateTime.now().plusHours(withdrawalLockHours));
        userRepository.save(user);
        log.info("Password updated for user {}. Withdrawals locked until {}", userId, user.getWithdrawalsDisabledUntil());
        try {
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                emailService.sendPasswordChangedEmail(user.getEmail());
            }
        } catch (Exception e) {
            log.warn("Failed to send password-changed email for user {}: {}", userId, e.getMessage());
        }
        return true;
    }

    /**
     * Update basic profile fields
     */
    @Transactional
    public Map<String, Object> updateProfile(UUID userId, com.UsdtWallet.UsdtWallet.controller.UserController.UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (req.fullName != null) user.setFullName(req.fullName.trim());
        if (req.phone != null) user.setPhone(req.phone.trim());
        if (req.avatar != null) user.setAvatar(req.avatar.trim());
        if (req.description != null) user.setDescription(req.description.trim());
        if (req.email != null && !req.email.trim().isEmpty()) user.setEmail(req.email.trim());

        user.setDateUpdated(LocalDateTime.now());
        userRepository.save(user);

        String walletAddress = getUserWalletAddress(user.getId());

        return Map.of(
            "id", user.getId().toString(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "fullName", user.getFullName() != null ? user.getFullName() : "",
            "phone", user.getPhone() != null ? user.getPhone() : "",
            "avatar", user.getAvatar() != null ? user.getAvatar() : "",
            "description", user.getDescription() != null ? user.getDescription() : "",
            "walletAddress", walletAddress != null ? walletAddress : "",
            "updatedAt", user.getDateUpdated()
        );
    }
}
