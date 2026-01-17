package com.example.back_end.modules.auth.service;

import com.example.back_end.modules.auth.entity.PasswordResetLog;
import com.example.back_end.modules.auth.entity.VerificationToken;
import com.example.back_end.modules.auth.repository.PasswordResetLogRepository;
import com.example.back_end.modules.auth.repository.VerificationTokenRepository;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Service for managing email verification and password reset tokens.
 *
 * <p>Security features:
 * <ul>
 *   <li>Tokens are cryptographically random (using SecureRandom)</li>
 *   <li>Tokens are hashed with SHA-256 before storage</li>
 *   <li>Tokens have configurable expiration times</li>
 *   <li>Used tokens are marked to prevent replay attacks</li>
 *   <li>Rate limiting to prevent abuse</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;
    private final PasswordResetLogRepository resetLogRepository;
    private final UserRepository userRepository;

    // Token configuration
    private static final int TOKEN_LENGTH = 32; // 32 bytes = 256 bits
    private static final long EMAIL_VERIFICATION_EXPIRY_HOURS = 24;
    private static final long PASSWORD_RESET_EXPIRY_HOURS = 1;
    private static final int MAX_TOKENS_PER_HOUR = 3; // Rate limit

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a cryptographically secure random token.
     *
     * @return Base64-encoded random token (URL-safe)
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Hash a token using SHA-256.
     *
     * @param token Plain text token
     * @return Hex-encoded SHA-256 hash
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
     * Create an email verification token for a user.
     *
     * @param user User entity
     * @return Plain text token (to be sent via email)
     * @throws IllegalStateException if rate limit exceeded
     */
    @Transactional
    public String createEmailVerificationToken(User user) {
        return createToken(user, VerificationToken.TokenType.EMAIL_VERIFICATION);
    }

    /**
     * Create a password reset token for a user.
     *
     * @param user User entity
     * @param ipAddress IP address of the requester
     * @param userAgent User agent string
     * @return Plain text token (to be sent via email)
     * @throws IllegalStateException if rate limit exceeded
     */
    @Transactional
    public String createPasswordResetToken(User user, String ipAddress, String userAgent) {
        String token = createToken(user, VerificationToken.TokenType.PASSWORD_RESET);

        // Log the request
        logPasswordReset(user, ipAddress, userAgent, PasswordResetLog.ResetStatus.REQUESTED,
                        "Password reset token generated");

        return token;
    }

    /**
     * Internal method to create a token.
     */
    private String createToken(User user, VerificationToken.TokenType tokenType) {
        // Check rate limit
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentTokenCount = tokenRepository.countActiveTokens(user, tokenType, LocalDateTime.now());

        if (recentTokenCount >= MAX_TOKENS_PER_HOUR) {
            log.warn("Rate limit exceeded for user {} trying to generate {} token",
                     user.getId(), tokenType);
            throw new IllegalStateException("Too many token requests. Please try again later.");
        }

        // Invalidate any existing unused tokens for this user and type
        int invalidatedCount = tokenRepository.invalidateUserTokens(user, tokenType, LocalDateTime.now());
        if (invalidatedCount > 0) {
            log.info("Invalidated {} existing {} tokens for user {}",
                     invalidatedCount, tokenType, user.getId());
        }

        // Generate new token
        String plainToken = generateSecureToken();
        String hashedToken = hashToken(plainToken);

        // Determine expiration time
        long expiryHours = tokenType == VerificationToken.TokenType.EMAIL_VERIFICATION
                           ? EMAIL_VERIFICATION_EXPIRY_HOURS
                           : PASSWORD_RESET_EXPIRY_HOURS;
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expiryHours);

        // Create and save token entity
        VerificationToken token = VerificationToken.builder()
                .user(user)
                .tokenHash(hashedToken)
                .tokenType(tokenType)
                .expiresAt(expiresAt)
                .build();

        tokenRepository.save(token);

        log.info("Created {} token for user {} (expires at {})",
                 tokenType, user.getId(), expiresAt);

        // Return the plain token (to be sent via email)
        return plainToken;
    }

    /**
     * Verify an email verification token and mark the user's email as verified.
     *
     * @param plainToken Plain text token from the email link
     * @return true if verification successful, false otherwise
     */
    @Transactional
    public boolean verifyEmailToken(String plainToken) {
        String hashedToken = hashToken(plainToken);

        Optional<VerificationToken> tokenOpt = tokenRepository.findValidToken(
            hashedToken,
            VerificationToken.TokenType.EMAIL_VERIFICATION,
            LocalDateTime.now()
        );

        if (tokenOpt.isEmpty()) {
            log.warn("Invalid or expired email verification token: {}", hashedToken.substring(0, 10) + "...");
            return false;
        }

        VerificationToken token = tokenOpt.get();
        User user = token.getUser();

        // Mark token as used
        token.markAsUsed();
        tokenRepository.save(token);

        // Mark user's email as verified
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Email verified successfully for user {}", user.getId());
        return true;
    }

    /**
     * Validate a password reset token (without marking it as used).
     *
     * @param plainToken Plain text token from the email link
     * @return Optional containing the user if token is valid
     */
    @Transactional(readOnly = true)
    public Optional<User> validatePasswordResetToken(String plainToken) {
        String hashedToken = hashToken(plainToken);

        Optional<VerificationToken> tokenOpt = tokenRepository.findValidToken(
            hashedToken,
            VerificationToken.TokenType.PASSWORD_RESET,
            LocalDateTime.now()
        );

        if (tokenOpt.isEmpty()) {
            log.warn("Invalid or expired password reset token");
            return Optional.empty();
        }

        return Optional.of(tokenOpt.get().getUser());
    }

    /**
     * Use a password reset token to change a user's password.
     *
     * @param plainToken Plain text token from the email link
     * @param newPasswordHash New password (already hashed with BCrypt)
     * @param ipAddress IP address of the requester
     * @param userAgent User agent string
     * @return true if password reset successful, false otherwise
     */
    @Transactional
    public boolean resetPassword(String plainToken, String newPasswordHash,
                                 String ipAddress, String userAgent) {
        String hashedToken = hashToken(plainToken);

        Optional<VerificationToken> tokenOpt = tokenRepository.findValidToken(
            hashedToken,
            VerificationToken.TokenType.PASSWORD_RESET,
            LocalDateTime.now()
        );

        if (tokenOpt.isEmpty()) {
            // Check if token exists but is expired/used
            Optional<VerificationToken> expiredTokenOpt = tokenRepository.findByTokenHash(hashedToken);
            if (expiredTokenOpt.isPresent()) {
                VerificationToken expiredToken = expiredTokenOpt.get();
                PasswordResetLog.ResetStatus status = expiredToken.isExpired()
                    ? PasswordResetLog.ResetStatus.EXPIRED
                    : PasswordResetLog.ResetStatus.FAILED;
                logPasswordReset(expiredToken.getUser(), ipAddress, userAgent, status,
                               "Token " + (expiredToken.isExpired() ? "expired" : "already used"));
            } else {
                log.warn("Password reset attempt with unknown token from IP {}", ipAddress);
            }
            return false;
        }

        VerificationToken token = tokenOpt.get();
        User user = token.getUser();

        // Mark token as used
        token.markAsUsed();
        tokenRepository.save(token);

        // Update password
        user.setPassword(newPasswordHash);
        userRepository.save(user);

        // Log successful reset
        logPasswordReset(user, ipAddress, userAgent, PasswordResetLog.ResetStatus.COMPLETED,
                        "Password reset completed successfully");

        log.info("Password reset successfully for user {}", user.getId());
        return true;
    }

    /**
     * Check if a user's email is verified.
     *
     * @param user User entity
     * @return true if email is verified
     */
    public boolean isEmailVerified(User user) {
        return Boolean.TRUE.equals(user.getEmailVerified());
    }

    /**
     * Resend email verification token.
     *
     * @param user User entity
     * @return Plain text token (to be sent via email)
     * @throws IllegalStateException if email already verified or rate limit exceeded
     */
    @Transactional
    public String resendEmailVerificationToken(User user) {
        if (isEmailVerified(user)) {
            throw new IllegalStateException("Email is already verified");
        }

        return createEmailVerificationToken(user);
    }

    /**
     * Clean up expired tokens (should be called by a scheduled task).
     *
     * @return Number of tokens deleted
     */
    @Transactional
    public int cleanupExpiredTokens() {
        int deletedCount = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired tokens", deletedCount);
        }
        return deletedCount;
    }

    /**
     * Log a password reset event.
     */
    private void logPasswordReset(User user, String ipAddress, String userAgent,
                                  PasswordResetLog.ResetStatus status, String notes) {
        PasswordResetLog logEntry = PasswordResetLog.builder()
                .user(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(status)
                .notes(notes)
                .build();

        resetLogRepository.save(logEntry);
    }

    /**
     * Check if a user has too many recent password reset requests.
     * Can be used for additional rate limiting checks.
     *
     * @param user User entity
     * @param hours Number of hours to look back
     * @param maxAttempts Maximum allowed attempts
     * @return true if rate limit exceeded
     */
    public boolean isPasswordResetRateLimitExceeded(User user, int hours, int maxAttempts) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        long attemptCount = resetLogRepository.findRecentAttemptsByUser(user, since).size();
        return attemptCount >= maxAttempts;
    }

    /**
     * Check if an IP address has too many recent password reset requests.
     * Can be used for IP-based rate limiting.
     *
     * @param ipAddress IP address
     * @param hours Number of hours to look back
     * @param maxAttempts Maximum allowed attempts
     * @return true if rate limit exceeded
     */
    public boolean isIpRateLimitExceeded(String ipAddress, int hours, int maxAttempts) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        long attemptCount = resetLogRepository.countRecentAttemptsByIp(ipAddress, since);
        return attemptCount >= maxAttempts;
    }
}

