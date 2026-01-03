package com.example.back_end.modules.terminal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a pairing code/request for terminal
 * Corresponds to terminal_pairing_codes table
 * Now supports both:
 * - Old flow: Direct pairing code generation (code_hash used)
 * - New flow: Request-approval workflow (request_token_hash + status)
 */
@Entity
@Table(name = "terminal_pairing_codes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalPairingCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "terminal_id", nullable = false)
    private Long terminalId;

    // ✅ code_hash is now nullable - used only in old flow
    @Column(name = "code_hash", columnDefinition = "TEXT")
    private String codeHash;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "issued_by")
    private Long issuedBy;

    // ✅ New fields for request-approval workflow
    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "request_token_hash", nullable = false, columnDefinition = "TEXT")
    private String requestTokenHash;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, APPROVED, USED, EXPIRED, REJECTED

    /**
     * Check if code is still valid (not used and not expired)
     * Used for old flow with code_hash
     */
    @Transient
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return usedAt == null && now.isBefore(expiresAt);
    }

    /**
     * Check if request/code is expired
     */
    @Transient
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if request is active (PENDING or APPROVED and not expired)
     * Used for new request-approval flow
     */
    @Transient
    public boolean isActive() {
        return ("PENDING".equals(status) || "APPROVED".equals(status)) && !isExpired();
    }

    /**
     * Mark code as used
     */
    public void markAsUsed() {
        this.usedAt = LocalDateTime.now();
        this.status = "USED";
    }

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
        // Default status if not set
        if (status == null) {
            status = "PENDING";
        }
    }
}