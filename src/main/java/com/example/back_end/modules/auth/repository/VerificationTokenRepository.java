package com.example.back_end.modules.auth.repository;

import com.example.back_end.modules.auth.entity.VerificationToken;
import com.example.back_end.modules.register.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for VerificationToken entity.
 * Handles email verification and password reset token operations.
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    /**
     * Find a valid (unused and non-expired) token by its hash and type.
     *
     * @param tokenHash SHA-256 hash of the token
     * @param tokenType Type of token (EMAIL_VERIFICATION or PASSWORD_RESET)
     * @return Optional containing the token if found and valid
     */
    @Query("""
        SELECT vt FROM VerificationToken vt
        WHERE vt.tokenHash = :tokenHash
          AND vt.tokenType = :tokenType
          AND vt.usedAt IS NULL
          AND vt.expiresAt > :now
    """)
    Optional<VerificationToken> findValidToken(
        @Param("tokenHash") String tokenHash,
        @Param("tokenType") VerificationToken.TokenType tokenType,
        @Param("now") LocalDateTime now
    );

    /**
     * Find a token by its hash (regardless of validity).
     * Useful for checking if a token exists even if expired/used.
     *
     * @param tokenHash SHA-256 hash of the token
     * @return Optional containing the token if found
     */
    Optional<VerificationToken> findByTokenHash(String tokenHash);

    /**
     * Find all tokens for a specific user and type.
     *
     * @param user User entity
     * @param tokenType Type of token
     * @return List of tokens
     */
    List<VerificationToken> findByUserAndTokenType(User user, VerificationToken.TokenType tokenType);

    /**
     * Find the latest unused token for a user and type.
     * Useful for preventing duplicate token generation.
     *
     * @param user User entity
     * @param tokenType Type of token
     * @return Optional containing the latest unused token
     */
    @Query("""
        SELECT vt FROM VerificationToken vt
        WHERE vt.user = :user
          AND vt.tokenType = :tokenType
          AND vt.usedAt IS NULL
        ORDER BY vt.createdAt DESC
        LIMIT 1
    """)
    Optional<VerificationToken> findLatestUnusedToken(
        @Param("user") User user,
        @Param("tokenType") VerificationToken.TokenType tokenType
    );

    /**
     * Delete all expired tokens.
     * This should be called periodically by a scheduled task for cleanup.
     *
     * @param now Current timestamp
     * @return Number of deleted tokens
     */
    @Modifying
    @Query("""
        DELETE FROM VerificationToken vt
        WHERE vt.expiresAt < :now
          AND vt.usedAt IS NULL
    """)
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Invalidate (mark as used) all existing tokens for a user and type.
     * Useful when generating a new token to ensure only one is active.
     *
     * @param user User entity
     * @param tokenType Type of token
     * @param now Current timestamp
     * @return Number of invalidated tokens
     */
    @Modifying
    @Query("""
        UPDATE VerificationToken vt
        SET vt.usedAt = :now
        WHERE vt.user = :user
          AND vt.tokenType = :tokenType
          AND vt.usedAt IS NULL
    """)
    int invalidateUserTokens(
        @Param("user") User user,
        @Param("tokenType") VerificationToken.TokenType tokenType,
        @Param("now") LocalDateTime now
    );

    /**
     * Count active (unused and non-expired) tokens for a user.
     * Useful for rate limiting token generation.
     *
     * @param user User entity
     * @param tokenType Type of token
     * @param now Current timestamp
     * @return Count of active tokens
     */
    @Query("""
        SELECT COUNT(vt) FROM VerificationToken vt
        WHERE vt.user = :user
          AND vt.tokenType = :tokenType
          AND vt.usedAt IS NULL
          AND vt.expiresAt > :now
    """)
    long countActiveTokens(
        @Param("user") User user,
        @Param("tokenType") VerificationToken.TokenType tokenType,
        @Param("now") LocalDateTime now
    );

    /**
     * Find all tokens created recently for a user (for rate limiting).
     *
     * @param user User entity
     * @param tokenType Type of token
     * @param since Timestamp to look back from
     * @return List of recent tokens
     */
    @Query("""
        SELECT vt FROM VerificationToken vt
        WHERE vt.user = :user
          AND vt.tokenType = :tokenType
          AND vt.createdAt > :since
        ORDER BY vt.createdAt DESC
    """)
    List<VerificationToken> findRecentTokens(
        @Param("user") User user,
        @Param("tokenType") VerificationToken.TokenType tokenType,
        @Param("since") LocalDateTime since
    );
}

