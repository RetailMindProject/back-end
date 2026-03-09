package com.example.back_end.modules.auth.entity;

import com.example.back_end.modules.register.entity.User.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for storing pending customer registrations awaiting email verification.
 *
 * <p>Registration flow:
 * <ol>
 *   <li>User submits registration form</li>
 *   <li>Pending registration created (this entity)</li>
 *   <li>Verification email sent with token</li>
 *   <li>User clicks verification link</li>
 *   <li>Token validated and user account created</li>
 *   <li>Pending registration deleted</li>
 * </ol>
 *
 * <p>This ensures users MUST verify email before account creation.
 */
@Entity
@Table(
    name = "pending_registrations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pending_registrations_email", columnNames = "email")
    },
    indexes = {
        @Index(name = "idx_pending_registrations_token_hash", columnList = "verification_token_hash"),
        @Index(name = "idx_pending_registrations_expires_at", columnList = "expires_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User's first name
     */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * User's last name
     */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Email address (must be unique in this table)
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Phone number (optional)
     */
    @Column(length = 20)
    private String phone;

    /**
     * Address (optional)
     */
    @Column(columnDefinition = "TEXT")
    private String address;

    /**
     * User role (typically CUSTOMER for self-registration)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    /**
     * Hashed password (BCrypt)
     */
    @Column(nullable = false, length = 255)
    private String passwordHash;

    /**
     * SHA-256 hash of the verification token
     */
    @Column(name = "verification_token_hash", nullable = false, length = 64)
    private String verificationTokenHash;

    /**
     * Token expiration timestamp (typically 24 hours from creation)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * When this pending registration was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if this pending registration has expired
     */
    @Transient
    public boolean isExpired() {
        if (expiresAt == null) {
            // If expiresAt is null, treat as expired for safety
            return true;
        }
        return expiresAt.isBefore(LocalDateTime.now());
    }
}

