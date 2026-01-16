package com.example.back_end.modules.terminal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a browser device paired with a terminal
 * Corresponds to terminal_devices table
 */
@Entity
@Table(name = "terminal_devices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "terminal_id", nullable = false)
    private Long terminalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", insertable = false, updatable = false)
    private Terminal terminal;

    @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "TEXT")
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    /**
     * Check if device is currently active (not revoked)
     */
    @Transient
    public boolean isActive() {
        return revokedAt == null;
    }

    /**
     * Revoke this device
     */
    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    /**
     * Update last seen timestamp
     */
    public void updateLastSeen() {
        this.lastSeenAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
        if (lastSeenAt == null) {
            lastSeenAt = LocalDateTime.now();
        }
    }
}