package com.example.back_end.modules.auth.controller;

import com.example.back_end.exception.CustomException;
import com.example.back_end.modules.auth.dto.EmailVerificationRequest;
import com.example.back_end.modules.auth.dto.PasswordResetConfirmRequest;
import com.example.back_end.modules.auth.dto.PasswordResetRequest;
import com.example.back_end.modules.auth.service.EmailService;
import com.example.back_end.modules.auth.service.PendingRegistrationService;
import com.example.back_end.modules.auth.service.VerificationTokenService;
import com.example.back_end.modules.register.dto.RegisterResponseDTO;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.repository.UserRepository;
import com.example.back_end.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for email verification and password reset operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/auth/verify-email - Verify email with token</li>
 *   <li>POST /api/auth/verify-registration - Complete registration via email verification</li>
 *   <li>POST /api/auth/resend-verification - Resend verification email</li>
 *   <li>POST /api/auth/forgot-password - Request password reset</li>
 *   <li>GET /api/auth/validate-reset-token - Validate reset token</li>
 *   <li>POST /api/auth/reset-password - Reset password with token</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationController {

    private final VerificationTokenService verificationTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PendingRegistrationService pendingRegistrationService;

    /**
     * Verify email address with token from email link.
     *
     * @param request Contains the verification token
     * @return Success or error message
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        log.info("Email verification attempt with token");

        boolean verified = verificationTokenService.verifyEmailToken(request.getToken());

        Map<String, Object> response = new HashMap<>();

        if (verified) {
            response.put("success", true);
            response.put("message", "Email verified successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid or expired token");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Complete customer registration via email verification.
     * Creates user account and customer record upon successful verification.
     *
     * @param request Contains the verification token
     * @return Registration response with user data and JWT token
     */
    @PostMapping("/verify-registration")
    public ResponseEntity<?> verifyRegistration(@Valid @RequestBody EmailVerificationRequest request) {
        log.info("Registration verification attempt - token received: {}", request.getToken() != null ? "yes" : "no");

        try {
            RegisterResponseDTO response = pendingRegistrationService.completeRegistration(request.getToken());
            log.info("Registration verification successful for user: {}", response.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (CustomException e) {
            log.error("Registration verification failed - CustomException: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Registration verification failed - Unexpected exception: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An error occurred during verification. Please try again or contact support.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Resend email verification token.
     * Requires authentication - user must be logged in.
     *
     * @param authentication Current authenticated user
     * @return Success or error message
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerification(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Get user email from authentication
            String email = authentication.getName();

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            User user = userOpt.get();

            // Check if already verified
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                response.put("success", false);
                response.put("message", "Email already verified");
                return ResponseEntity.badRequest().body(response);
            }

            // Generate new token
            String token = verificationTokenService.resendEmailVerificationToken(user);

            // Send email
            String userName = user.getFirstName() != null ? user.getFirstName() : "User";
            emailService.sendEmailVerification(user.getEmail(), userName, token);

            log.info("Verification email sent to: {}", user.getEmail());

            response.put("success", true);
            response.put("message", "Verification email sent");
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // Rate limit exceeded or already verified
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        } catch (Exception e) {
            log.error("Error resending verification email", e);
            response.put("success", false);
            response.put("message", "An error occurred");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Request password reset.
     * Always returns success to prevent email enumeration.
     *
     * @param request Contains email address
     * @param httpRequest HTTP request for IP/User-Agent extraction
     * @return Success message (always, for security)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {

        log.info("Password reset requested for email: {}", request.getEmail());

        Map<String, Object> response = new HashMap<>();

        try {
            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String ipAddress = getClientIp(httpRequest);
                String userAgent = httpRequest.getHeader("User-Agent");

                // Check rate limit
                if (verificationTokenService.isPasswordResetRateLimitExceeded(user, 1, 5)) {
                    // Still return success for security
                    response.put("success", true);
                    response.put("message", "If an account exists with this email, you will receive a password reset link");
                    return ResponseEntity.ok(response);
                }

                // Generate reset token
                String token = verificationTokenService.createPasswordResetToken(user, ipAddress, userAgent);

                // Send email
                String userName = user.getFirstName() != null ? user.getFirstName() : "User";
                emailService.sendPasswordReset(user.getEmail(), userName, token);

                log.info("Password reset email sent to: {}", user.getEmail());
            } else {
                log.info("Password reset requested for non-existent email: {}", request.getEmail());
            }

            // Always return success (prevent email enumeration)
            response.put("success", true);
            response.put("message", "If an account exists with this email, you will receive a password reset link");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing password reset request", e);
            // Still return success for security
            response.put("success", true);
            response.put("message", "If an account exists with this email, you will receive a password reset link");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Validate password reset token (pre-check before showing form).
     *
     * @param token Reset token from email link
     * @return Token validity status
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<Map<String, Object>> validateResetToken(@RequestParam String token) {
        log.info("Validating password reset token");

        Map<String, Object> response = new HashMap<>();

        Optional<User> userOpt = verificationTokenService.validatePasswordResetToken(token);

        if (userOpt.isPresent()) {
            response.put("valid", true);
            response.put("message", "Token is valid");
            return ResponseEntity.ok(response);
        } else {
            response.put("valid", false);
            response.put("message", "Invalid or expired token");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Reset password with token.
     *
     * @param request Contains token, new password, and confirmation
     * @param httpRequest HTTP request for IP/User-Agent extraction
     * @return Success or error message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request,
            HttpServletRequest httpRequest) {

        log.info("Password reset attempt with token");

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate passwords match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                response.put("success", false);
                response.put("message", "Passwords do not match");
                return ResponseEntity.badRequest().body(response);
            }

            // Hash new password
            String hashedPassword = passwordEncoder.encode(request.getNewPassword());

            // Get client info
            String ipAddress = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            // Perform password reset
            boolean success = verificationTokenService.resetPassword(
                request.getToken(),
                hashedPassword,
                ipAddress,
                userAgent
            );

            if (success) {
                response.put("success", true);
                response.put("message", "Password reset successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Invalid or expired token");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error resetting password", e);
            response.put("success", false);
            response.put("message", "An error occurred");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Extract client IP address from request.
     * Handles proxies and load balancers.
     *
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}

