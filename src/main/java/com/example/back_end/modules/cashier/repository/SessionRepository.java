package com.example.back_end.modules.cashier.repository;

import com.example.back_end.modules.cashier.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    // ========================================
    // ✅ Terminal-based validation (Required)
    // ========================================

    @Query("SELECT s FROM Session s WHERE s.terminalId = :terminalId AND s.status = 'OPEN'")
    Optional<Session> findOpenSessionByTerminalId(@Param("terminalId") Long terminalId);

    // ========================================
    // ✅ History / Listing (Allowed)
    // ========================================

    List<Session> findByUserIdOrderByOpenedAtDesc(Long userId);

    // ========================================
    // ✅ Existing Dashboard Queries
    // ========================================

    @Query("SELECT s FROM Session s ORDER BY s.openedAt DESC")
    List<Session> findAllSessionsOrdered();

    @Query("SELECT s FROM Session s WHERE s.status = 'OPEN' ORDER BY s.openedAt DESC")
    List<Session> findAllActiveSessions();

    @Query("SELECT s FROM Session s WHERE s.status = 'CLOSED' ORDER BY s.openedAt DESC")
    List<Session> findAllClosedSessions();

    @Query("SELECT s FROM Session s WHERE s.id = :sessionId")
    Optional<Session> findByIdWithUser(@Param("sessionId") Long sessionId);
}
