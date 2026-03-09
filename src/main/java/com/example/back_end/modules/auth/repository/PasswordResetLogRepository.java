package com.example.back_end.modules.auth.repository;

import com.example.back_end.modules.auth.entity.PasswordResetLog;
import com.example.back_end.modules.register.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for PasswordResetLog entity.
 * Provides audit trail for password reset operations.
 */
@Repository
public interface PasswordResetLogRepository extends JpaRepository<PasswordResetLog, Long> {

    /**
     * Find all reset logs for a specific user, ordered by most recent first.
     *
     * @param user User entity
     * @return List of password reset logs
     */
    List<PasswordResetLog> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find recent reset attempts from a specific IP address.
     * Useful for detecting abuse or brute force attempts.
     *
     * @param ipAddress IP address
     * @param since Timestamp to look back from
     * @return List of recent reset attempts
     */
    @Query("""
        SELECT prl FROM PasswordResetLog prl
        WHERE prl.ipAddress = :ipAddress
          AND prl.createdAt > :since
        ORDER BY prl.createdAt DESC
    """)
    List<PasswordResetLog> findRecentAttemptsByIp(
        @Param("ipAddress") String ipAddress,
        @Param("since") LocalDateTime since
    );

    /**
     * Find recent reset attempts for a specific user.
     * Useful for rate limiting and security monitoring.
     *
     * @param user User entity
     * @param since Timestamp to look back from
     * @return List of recent reset attempts
     */
    @Query("""
        SELECT prl FROM PasswordResetLog prl
        WHERE prl.user = :user
          AND prl.createdAt > :since
        ORDER BY prl.createdAt DESC
    """)
    List<PasswordResetLog> findRecentAttemptsByUser(
        @Param("user") User user,
        @Param("since") LocalDateTime since
    );

    /**
     * Count recent failed attempts for a user.
     * Useful for account lockout policies.
     *
     * @param user User entity
     * @param since Timestamp to look back from
     * @return Count of failed attempts
     */
    @Query("""
        SELECT COUNT(prl) FROM PasswordResetLog prl
        WHERE prl.user = :user
          AND prl.status = 'FAILED'
          AND prl.createdAt > :since
    """)
    long countRecentFailedAttempts(
        @Param("user") User user,
        @Param("since") LocalDateTime since
    );

    /**
     * Count recent reset requests from an IP address.
     * Useful for detecting abuse.
     *
     * @param ipAddress IP address
     * @param since Timestamp to look back from
     * @return Count of recent requests
     */
    @Query("""
        SELECT COUNT(prl) FROM PasswordResetLog prl
        WHERE prl.ipAddress = :ipAddress
          AND prl.createdAt > :since
    """)
    long countRecentAttemptsByIp(
        @Param("ipAddress") String ipAddress,
        @Param("since") LocalDateTime since
    );
}

