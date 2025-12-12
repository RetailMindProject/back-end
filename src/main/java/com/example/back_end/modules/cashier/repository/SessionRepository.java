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
    // 🆕 Methods للـ Terminal Operations (مطلوبة!)
    // ========================================

    /**
     * Find open session by terminal ID
     * Used to check if terminal already has an open session
     */
    @Query("SELECT s FROM Session s WHERE s.terminal.id = :terminalId AND s.status = 'OPEN'")
    Optional<Session> findOpenSessionByTerminalId(@Param("terminalId") Long terminalId);

    /**
     * Find open session by user ID
     * Used to check if user already has an open session
     */
    @Query("SELECT s FROM Session s WHERE s.user.id = :userId AND s.status = 'OPEN'")
    Optional<Session> findOpenSessionByUserId(@Param("userId") Integer userId);

    /**
     * Find all sessions by user (ordered by date)
     * Used for session history
     */
    List<Session> findByUserIdOrderByOpenedAtDesc(Long userId);

    // ========================================
    // ✅ Methods الموجودة عندك (Dashboard)
    // ========================================

    /**
     * ✅ Using JPQL (not native SQL)
     * ✅ No JOIN FETCH to avoid type issues
     * ✅ Lazy loading will get User when needed
     */
    @Query("SELECT s FROM Session s ORDER BY s.openedAt DESC")
    List<Session> findAllSessionsOrdered();

    @Query("SELECT s FROM Session s WHERE s.status = 'OPEN' ORDER BY s.openedAt DESC")
    List<Session> findAllActiveSessions();

    @Query("SELECT s FROM Session s WHERE s.status = 'CLOSED' ORDER BY s.openedAt DESC")
    List<Session> findAllClosedSessions();

    @Query("SELECT s FROM Session s WHERE s.id = :sessionId")
    Optional<Session> findByIdWithUser(@Param("sessionId") Long sessionId);
}