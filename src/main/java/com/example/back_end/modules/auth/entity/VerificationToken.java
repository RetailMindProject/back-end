package com.example.back_end.modules.auth.entity;

import com.example.back_end.modules.register.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for storing verification tokens (email verification and password reset).
 * Tokens are stored as SHA-256 hashes for security.
 *
 * <p>Security considerations:
 * <ul>
 *   <li>Never store plain text tokens in the database</li>
 *   <li>Always hash tokens using SHA-256 before storing</li>
 *   <li>Tokens should have short expiration times (24h for email, 1h for password reset)</li>
 *   <li>Mark tokens as used after consumption to prevent replay attacks</li>
 * </ul>
 */
@Entity
@Table(
    name = "verification_tokens",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_verification_tokens_user_type",
            columnNames = {"user_id", "token_type", "used_at"}
        )
    },
    indexes = {
        @Index(name = "idx_verification_tokens_hash", columnList = "token_hash"),
        @Index(name = "idx_verification_tokens_expires_at", columnList = "expires_at"),
        @Index(name = "idx_verification_tokens_user_id", columnList = "user_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_verification_tokens_user"))
    private User user;

    /**
     * SHA-256 hash of the actual token (64 hex characters).
     * Never store the plain token in the database.
     */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 30)
    private TokenType tokenType;

    /**
     * Token expiration timestamp.
     * Recommended values:
     * - EMAIL_VERIFICATION: 24 hours
     * - PASSWORD_RESET: 1 hour
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Timestamp when the token was used.
     * NULL means the token hasn't been used yet.
     * Once set, the token cannot be reused (prevents replay attacks).
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Token type enumeration
     */
    public enum TokenType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }

    /**
     * Check if the token is still valid (not expired and not used)
     */
    @Transient
    public boolean isValid() {
        return usedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Check if the token has expired
     */
    @Transient
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Check if the token has been used
     */
    @Transient
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Mark the token as used
     */
    public void markAsUsed() {
        this.usedAt = LocalDateTime.now();
    }
}

