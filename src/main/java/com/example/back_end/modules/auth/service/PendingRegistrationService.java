package com.example.back_end.modules.auth.service;

import com.example.back_end.exception.CustomException;
import com.example.back_end.modules.auth.entity.PendingRegistration;
import com.example.back_end.modules.auth.repository.PendingRegistrationRepository;
import com.example.back_end.modules.register.dto.RegisterRequestDTO;
import com.example.back_end.modules.register.dto.RegisterResponseDTO;
import com.example.back_end.modules.register.entity.Customer;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.entity.User.UserRole;
import com.example.back_end.modules.register.mapper.UserMapper;
import com.example.back_end.modules.register.repository.CustomerRepository;
import com.example.back_end.modules.register.repository.UserRepository;
import com.example.back_end.security.JwtService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Service for handling new customer registration with mandatory email verification.
 *
 * <p>Registration flow:
 * <ol>
 *   <li>User submits registration → creates pending_registration record</li>
 *   <li>Verification email sent with token</li>
 *   <li>User clicks link → validates token</li>
 *   <li>Creates user account + customer record</li>
 *   <li>Deletes pending registration</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PendingRegistrationService {

    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final EntityManager entityManager;

    private static final int TOKEN_LENGTH = 32; // 32 bytes = 256 bits
    private static final long EMAIL_VERIFICATION_EXPIRY_HOURS = 24;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a cryptographically secure random token.
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Hash a token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Start customer registration process (creates pending registration).
     * Does NOT create user account yet - requires email verification first.
     *
     * @param request Registration request DTO
     * @return Response indicating verification email was sent
     */
    @Transactional
    public RegisterResponseDTO startCustomerRegistration(RegisterRequestDTO request) {

        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new CustomException("Passwords do not match");
        }

        // Check if email already exists in users table (existing users)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already registered");
        }

        // Check if email already exists in customers table
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already registered");
        }

        // Check if phone already exists in customers table
        if (request.getPhone() != null && customerRepository.existsByPhone(request.getPhone())) {
            throw new CustomException("Phone already registered");
        }

        // Check if there's already a pending registration for this email
        Optional<PendingRegistration> existingPending = pendingRegistrationRepository.findByEmail(request.getEmail());

        if (existingPending.isPresent()) {
            PendingRegistration pending = existingPending.get();

            // If expired, delete it and allow new registration
            if (pending.isExpired()) {
                pendingRegistrationRepository.delete(pending);
                log.info("Deleted expired pending registration for email: {}", request.getEmail());
            } else {
                // If not expired, reject (user should check their email)
                throw new CustomException("A verification email has already been sent to this address. Please check your inbox or wait for the link to expire.");
            }
        }

        // Generate verification token
        String plainToken = generateSecureToken();
        String hashedToken = hashToken(plainToken);

        // Calculate expiration time
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(EMAIL_VERIFICATION_EXPIRY_HOURS);

        log.info("Creating pending registration - Current time: {}, Expiry time: {}, Hours until expiry: {}",
                 now, expiresAt, EMAIL_VERIFICATION_EXPIRY_HOURS);

        // Create pending registration
        PendingRegistration pendingRegistration = PendingRegistration.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(UserRole.CUSTOMER) // Always CUSTOMER for self-registration
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .verificationTokenHash(hashedToken)
                .expiresAt(expiresAt)
                .build();

        PendingRegistration saved = pendingRegistrationRepository.save(pendingRegistration);
        entityManager.flush(); // Force immediate write
        entityManager.refresh(saved); // Reload from DB to confirm what was actually saved

        log.info("Pending registration saved - ID: {}, ExpiresAt sent: {}, ExpiresAt from DB: {}",
                 saved.getId(), expiresAt, saved.getExpiresAt());

        // Critical validation
        if (saved.getExpiresAt().equals(now) || saved.getExpiresAt().isBefore(now)) {
            log.error("CRITICAL BUG DETECTED: expiresAt was saved incorrectly!");
            log.error("Expected (future): {}", expiresAt);
            log.error("Actual (from DB): {}", saved.getExpiresAt());
            log.error("Current time: {}", now);
            log.error("Difference: {} hours", java.time.Duration.between(saved.getExpiresAt(), expiresAt).toHours());
        } else {
            log.info("ExpiresAt validation PASSED - token will expire in {} hours",
                     java.time.Duration.between(now, saved.getExpiresAt()).toHours());
        }

        // Send verification email with correct link to /verify-registration
        String userName = request.getFirstName() != null ? request.getFirstName() : "User";
        emailService.sendRegistrationVerification(request.getEmail(), userName, plainToken);

        log.info("Pending registration created for email: {}. Verification email sent.", request.getEmail());

        // Return response (no JWT token yet - account not created)
        return RegisterResponseDTO.builder()
                .message("Registration initiated. Please check your email to verify your account.")
                .token(null) // No token until verification complete
                .id(null)
                .firstName(null)
                .lastName(null)
                .email(null)
                .phone(null)
                .address(null)
                .role(null)
                .isActive(null)
                .createdAt(null)
                .build();
    }

    /**
     * Complete registration by verifying email token.
     * Creates the actual user account and customer record.
     *
     * @param token Plain text verification token from email
     * @return Response with user data and JWT token
     */
    @Transactional
    public RegisterResponseDTO completeRegistration(String token) {
        log.info("=== STARTING REGISTRATION COMPLETION ===");
        log.info("Token received - length: {}, last 8 chars: {}",
                 token != null ? token.length() : 0,
                 token != null && token.length() > 8 ? "..." + token.substring(token.length() - 8) : "N/A");

        try {
            String hashedToken = hashToken(token);
            log.info("Step 1: Token hashed successfully (SHA-256 hash length: {})", hashedToken.length());

            // Find pending registration by token hash
            Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository.findByVerificationTokenHash(hashedToken);

            if (pendingOpt.isEmpty()) {
                log.warn("Step 2 FAILED: No matching pending registration found for hashed token");
                log.warn("This could mean: (1) Token is invalid, (2) Already used, or (3) Expired and cleaned up");
                throw new CustomException("Invalid or expired verification link");
            }

            PendingRegistration pending = pendingOpt.get();
            log.info("Step 2: Found pending registration - ID: {}, email: {}, role: {}",
                     pending.getId(), pending.getEmail(), pending.getRole());

            // Check if expired - CRITICAL CHECK
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = pending.getExpiresAt();
            LocalDateTime createdAt = pending.getCreatedAt();

            log.info("Step 3 - Timestamp Analysis:");
            log.info("  Created at:  {}", createdAt);
            log.info("  Expires at:  {}", expiresAt);
            log.info("  Current now: {}", now);
            log.info("  Time since creation: {} minutes", java.time.Duration.between(createdAt, now).toMinutes());
            log.info("  Time until expiry:   {} minutes", java.time.Duration.between(now, expiresAt).toMinutes());

            if (expiresAt == null) {
                log.error("Step 3 CRITICAL: expiresAt is NULL for pending registration! Email: {}", pending.getEmail());
                pendingRegistrationRepository.delete(pending);
                throw new CustomException("Invalid pending registration - missing expiration date. Please register again.");
            }

            boolean isExpired = expiresAt.isBefore(now) || expiresAt.equals(now);
            log.info("Step 3 - Expiration Decision: expiresAt.isBefore(now) = {}, isExpired = {}",
                     expiresAt.isBefore(now), isExpired);

            if (isExpired) {
                log.warn("Step 3 FAILED: Pending registration EXPIRED for email: {}", pending.getEmail());
                log.warn("  Registration was created {} hours ago", java.time.Duration.between(createdAt, now).toHours());
                log.warn("  Token expired {} minutes ago", java.time.Duration.between(expiresAt, now).toMinutes());
                log.warn("  Expected expiry duration: {} hours", EMAIL_VERIFICATION_EXPIRY_HOURS);
                pendingRegistrationRepository.delete(pending);
                throw new CustomException("Verification link has expired. Please register again.");
            }

            long hoursRemaining = java.time.Duration.between(now, expiresAt).toHours();
            long minutesRemaining = java.time.Duration.between(now, expiresAt).toMinutes();
            log.info("Step 3 PASSED: Token is VALID - {} hours and {} minutes remaining",
                     hoursRemaining, minutesRemaining % 60);

            // Double-check email doesn't exist (race condition protection)
            boolean emailExists = userRepository.existsByEmail(pending.getEmail());
            if (emailExists) {
                log.warn("Step 4 FAILED: Email already exists in users table: {}", pending.getEmail());
                pendingRegistrationRepository.delete(pending);
                throw new CustomException("Email already registered");
            }
            log.info("Step 4: Email not found in users table - proceeding with account creation");

            // Create user account
            log.info("Step 5: Creating User entity object");
            User user = new User();
            user.setFirstName(pending.getFirstName());
            user.setLastName(pending.getLastName());
            user.setEmail(pending.getEmail());
            user.setPhone(pending.getPhone());
            user.setAddress(pending.getAddress());
            user.setRole(pending.getRole());
            user.setPassword(pending.getPasswordHash());
            user.setIsActive(true);
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            log.info("Step 5 COMPLETE: User entity created - email: {}, role: {}, isActive: {}, emailVerified: {}",
                user.getEmail(), user.getRole(), user.getIsActive(), user.getEmailVerified());

            log.info("Step 6: Saving User to database...");
            User savedUser;
            try {
                savedUser = userRepository.save(user);
                log.info("Step 6a: userRepository.save() completed");

                entityManager.flush();
                log.info("Step 6b: entityManager.flush() completed");

                log.info("Step 6 COMPLETE: User saved - ID: {}, email: {}, isActive: {}, emailVerified: {}",
                    savedUser.getId(), savedUser.getEmail(), savedUser.getIsActive(), savedUser.getEmailVerified());
            } catch (Exception e) {
                log.error("Step 6 FAILED: Exception during user save", e);
                log.error("Exception type: {}", e.getClass().getName());
                log.error("Exception message: {}", e.getMessage());
                if (e.getCause() != null) {
                    log.error("Cause type: {}", e.getCause().getClass().getName());
                    log.error("Cause message: {}", e.getCause().getMessage());
                }
                throw new CustomException("Failed to create user account: " + e.getMessage());
            }

            // Verify user ID
            if (savedUser.getId() == null) {
                log.error("Step 7 FAILED: User saved but ID is null!");
                throw new CustomException("Failed to create user account - no ID generated");
            }
            log.info("Step 7: User ID verified: {}", savedUser.getId());

            // Create customer record
            log.info("Step 8: Creating Customer entity object");
            Customer customer = new Customer();
            customer.setFirstName(savedUser.getFirstName());
            customer.setLastName(savedUser.getLastName());
            customer.setPhone(savedUser.getPhone());
            customer.setEmail(savedUser.getEmail());
            customer.setLastVisitedAt(LocalDateTime.now());
            customer.setUserId(savedUser.getId());
            log.info("Step 8 COMPLETE: Customer entity created - email: {}, userId: {}", customer.getEmail(), customer.getUserId());

            log.info("Step 9: Saving Customer to database...");
            Customer savedCustomer;
            try {
                savedCustomer = customerRepository.save(customer);
                log.info("Step 9a: customerRepository.save() completed");

                entityManager.flush();
                log.info("Step 9b: entityManager.flush() completed");

                log.info("Step 9 COMPLETE: Customer saved - ID: {}, email: {}, userId: {}",
                    savedCustomer.getId(), savedCustomer.getEmail(), savedCustomer.getUserId());
            } catch (Exception e) {
                log.error("Step 9 FAILED: Exception during customer save", e);
                log.error("Exception type: {}", e.getClass().getName());
                log.error("Exception message: {}", e.getMessage());
                if (e.getCause() != null) {
                    log.error("Cause type: {}", e.getCause().getClass().getName());
                    log.error("Cause message: {}", e.getCause().getMessage());
                }
                throw new CustomException("Failed to create customer record: " + e.getMessage());
            }

            // Delete pending registration
            log.info("Step 10: Deleting pending registration...");
            try {
                pendingRegistrationRepository.delete(pending);
                log.info("Step 10 COMPLETE: Pending registration deleted for email: {}", pending.getEmail());
            } catch (Exception e) {
                log.error("Step 10 WARNING: Failed to delete pending registration (non-critical)", e);
            }

            // Generate JWT token
            log.info("Step 11: Generating JWT token...");
            String jwtToken;
            try {
                jwtToken = jwtService.generateToken(
                        savedUser.getEmail(),
                        savedUser.getRole().name(),
                        savedUser.getId(),
                        savedUser.getFirstName(),
                        savedUser.getLastName()
                );
                log.info("Step 11 COMPLETE: JWT token generated");
            } catch (Exception e) {
                log.error("Step 11 FAILED: Exception during JWT generation", e);
                throw new CustomException("Failed to generate authentication token: " + e.getMessage());
            }

            // Map to response DTO
            log.info("Step 12: Mapping to RegisterResponseDTO...");
            RegisterResponseDTO response;
            try {
                response = userMapper.toRegisterResponseDTO(savedUser, jwtToken);
                log.info("Step 12 COMPLETE: Response DTO created");
            } catch (Exception e) {
                log.error("Step 12 FAILED: Exception during DTO mapping", e);
                throw new CustomException("Failed to create response: " + e.getMessage());
            }

            log.info("=== REGISTRATION COMPLETION SUCCESS ===");
            log.info("User created - ID: {}, Email: {}, Role: {}", savedUser.getId(), savedUser.getEmail(), savedUser.getRole());
            log.info("Customer created - ID: {}", savedCustomer.getId());
            log.info("Transaction will commit when method returns");

            return response;

        } catch (CustomException e) {
            log.error("=== REGISTRATION COMPLETION FAILED (CustomException) ===");
            log.error("Error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("=== REGISTRATION COMPLETION FAILED (Unexpected Exception) ===");
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage(), e);
            throw new CustomException("Unexpected error during registration: " + e.getMessage());
        }
    }

    /**
     * Clean up expired pending registrations.
     * Should be called by a scheduled task.
     */
    @Transactional
    public int cleanupExpiredPendingRegistrations() {
        int deletedCount = pendingRegistrationRepository.deleteExpired(LocalDateTime.now());
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired pending registrations", deletedCount);
        }
        return deletedCount;
    }
}

