package com.example.back_end.modules.terminal.repository;

import com.example.back_end.modules.terminal.entity.TerminalPairingCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TerminalPairingCode entity
 * Supports both old flow (code_hash) and new flow (request-approval)
 */
@Repository
public interface TerminalPairingCodeRepository extends JpaRepository<TerminalPairingCode, Long> {

    // ========================================
    // OLD FLOW METHODS (keep for backward compatibility)
    // ========================================

    /**
     * Find valid (unused and not expired) pairing code by code hash
     *
     * @param codeHash Hashed pairing code
     * @param now      Current timestamp
     * @return Optional of TerminalPairingCode
     */
    @Query("SELECT tpc FROM TerminalPairingCode tpc " +
            "WHERE tpc.codeHash = :codeHash " +
            "AND tpc.usedAt IS NULL " +
            "AND tpc.expiresAt > :now")
    Optional<TerminalPairingCode> findValidByCodeHash(
            @Param("codeHash") String codeHash,
            @Param("now") LocalDateTime now
    );

    /**
     * Find active (unused) pairing code for a terminal
     *
     * @param terminalId Terminal ID
     * @return Optional of TerminalPairingCode
     */
    @Query("SELECT tpc FROM TerminalPairingCode tpc " +
            "WHERE tpc.terminalId = :terminalId " +
            "AND tpc.usedAt IS NULL")
    Optional<TerminalPairingCode> findActiveByTerminalId(@Param("terminalId") Long terminalId);

    /**
     * Check if terminal has any active (unused) pairing code
     *
     * @param terminalId Terminal ID
     * @return true if exists
     */
    @Query("SELECT COUNT(tpc) > 0 FROM TerminalPairingCode tpc " +
            "WHERE tpc.terminalId = :terminalId " +
            "AND tpc.usedAt IS NULL")
    boolean existsActiveByTerminalId(@Param("terminalId") Long terminalId);

    /**
     * Mark all active pairing codes for a terminal as used
     *
     * @param terminalId Terminal ID
     * @param now        Current timestamp
     */
    @Modifying
    @Query("UPDATE TerminalPairingCode tpc SET tpc.usedAt = :now " +
            "WHERE tpc.terminalId = :terminalId AND tpc.usedAt IS NULL")
    void markAllAsUsedForTerminal(
            @Param("terminalId") Long terminalId,
            @Param("now") LocalDateTime now
    );

    // ========================================
    // NEW REQUEST-APPROVAL FLOW METHODS
    // ========================================

    /**
     * Find pending requests (not expired)
     */
    @Query("SELECT tpc FROM TerminalPairingCode tpc " +
            "WHERE tpc.status = 'PENDING' " +
            "AND tpc.expiresAt > :now " +
            "ORDER BY tpc.issuedAt DESC")
    List<TerminalPairingCode> findPendingRequests(@Param("now") LocalDateTime now);

    /**
     * Find latest request for specific terminal and browser token
     */
    @Query("SELECT tpc FROM TerminalPairingCode tpc " +
            "WHERE tpc.terminalId = :terminalId " +
            "AND tpc.requestTokenHash = :tokenHash " +
            "ORDER BY tpc.issuedAt DESC")
    List<TerminalPairingCode> findLatestByTerminalAndToken(
            @Param("terminalId") Long terminalId,
            @Param("tokenHash") String tokenHash,
            org.springframework.data.domain.Pageable pageable
    );



    /**
     * Find active request for cashier (PENDING or APPROVED, not expired)
     */
    @Query("SELECT tpc FROM TerminalPairingCode tpc " +
            "WHERE tpc.requestedBy = :userId " +
            "AND tpc.status IN ('PENDING', 'APPROVED') " +
            "AND tpc.expiresAt > :now " +
            "ORDER BY tpc.issuedAt DESC")
    Optional<TerminalPairingCode> findActiveRequestByUser(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    /**
     * Check if terminal has active request (PENDING or APPROVED)
     */
    @Query("SELECT COUNT(tpc) > 0 FROM TerminalPairingCode tpc " +
            "WHERE tpc.terminalId = :terminalId " +
            "AND tpc.status IN ('PENDING', 'APPROVED') " +
            "AND tpc.expiresAt > :now")
    boolean hasActiveRequest(
            @Param("terminalId") Long terminalId,
            @Param("now") LocalDateTime now
    );

    @Transactional
    @Modifying
    @Query("UPDATE TerminalPairingCode tpc " +
            "SET tpc.status = 'EXPIRED' " +
            "WHERE tpc.status IN ('PENDING', 'APPROVED') " +
            "AND tpc.expiresAt < :now")
    int markExpiredRequests(@Param("now") LocalDateTime now);
}
