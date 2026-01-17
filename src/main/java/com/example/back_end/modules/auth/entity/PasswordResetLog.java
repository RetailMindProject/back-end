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
 * Audit log entity for tracking password reset attempts.
 * Used for security monitoring and fraud detection.
 */
@Entity
@Table(
    name = "password_reset_log",
    indexes = {
        @Index(name = "idx_password_reset_log_user_id", columnList = "user_id,created_at"),
        @Index(name = "idx_password_reset_log_ip", columnList = "ip_address,created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_password_reset_log_user"))
    private User user;

    /**
     * IP address from which the reset was requested/completed.
     * Supports both IPv4 and IPv6 (max 45 chars for IPv6).
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string from the HTTP request.
     */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /**
     * Status of the password reset attempt.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResetStatus status;

    /**
     * Additional context or reason for the status.
     * Useful for debugging and security analysis.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Password reset status enumeration
     */
    public enum ResetStatus {
        /** Password reset token was requested and sent */
        REQUESTED,
        /** Password was successfully changed using the token */
        COMPLETED,
        /** Failed attempt (invalid token, wrong email, etc.) */
        FAILED,
        /** Token expired before use */
        EXPIRED
    }
}

