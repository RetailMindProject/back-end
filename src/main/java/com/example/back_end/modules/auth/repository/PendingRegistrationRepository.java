package com.example.back_end.modules.auth.repository;

import com.example.back_end.modules.auth.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for PendingRegistration entity.
 */
@Repository
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {

    /**
     * Find pending registration by email.
     */
    Optional<PendingRegistration> findByEmail(String email);

    /**
     * Find pending registration by verification token hash.
     */
    Optional<PendingRegistration> findByVerificationTokenHash(String tokenHash);

    /**
     * Check if email exists in pending registrations.
     */
    boolean existsByEmail(String email);

    /**
     * Delete expired pending registrations.
     * Should be called by a scheduled cleanup task.
     */
    @Modifying
    @Query("DELETE FROM PendingRegistration pr WHERE pr.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}

