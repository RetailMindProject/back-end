package com.example.back_end.modules.cashier.service;

import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.cashier.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionLifecycleService {

    private static final long SESSION_LIFETIME_HOURS = 24;
    private static final BigDecimal DEFAULT_OPENING_FLOAT = new BigDecimal("2000.00");

    private final SessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public Session getCurrentSession(Long terminalId) {
        return sessionRepository.findOpenSessionByTerminalId(terminalId).orElse(null);
    }

    @Transactional
    public Session ensureSessionForLogin(Long terminalId, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required to open a session");
        }

        Session currentSession = sessionRepository.findOpenSessionByTerminalId(terminalId).orElse(null);

        if (currentSession == null) {
            log.info("No open session for terminal {}. Creating new session for user {}.", terminalId, userId);
            return createNewSession(terminalId, userId);
        }

        if (isSessionExpired(currentSession)) {
            log.info("Session {} is older than {} hours. Rotating session.", currentSession.getId(), SESSION_LIFETIME_HOURS);
            return rotateSession(currentSession, terminalId, userId);
        }

        if (currentSession.getUserId() == null) {
            currentSession.setUserId(userId);
            sessionRepository.save(currentSession);
            log.info("Updated session {} with userId {}", currentSession.getId(), userId);
        }

        return currentSession;
    }

    @Transactional
    public Session forceRotateSession(Long terminalId) {
        Session currentSession = sessionRepository.findOpenSessionByTerminalId(terminalId)
                .orElseThrow(() -> new IllegalStateException("No open session to rotate"));

        Long userId = currentSession.getUserId();
        if (userId == null) {
            throw new IllegalStateException("Cannot rotate session without userId");
        }

        return rotateSession(currentSession, terminalId, userId);
    }
    @Transactional
    public Session ensureValidSession(Long terminalId, Long userId) {
        log.info("Ensuring valid session for terminal {} and user {}", terminalId, userId);

        // ✅ Check for OPEN session on THIS terminal only
        Optional<Session> existingSession = sessionRepository
                .findOpenSessionByTerminalId(terminalId);

        if (existingSession.isPresent()) {
            Session session = existingSession.get();

            // ✅ Update userId if provided and different
            if (userId != null && !userId.equals(session.getUserId())) {
                session.setUserId(userId);
                sessionRepository.save(session);
            }

            log.info("Found existing open session {} for terminal {}",
                    session.getId(), terminalId);
            return session;
        }

        // ✅ No check for user's other sessions - مسموح
        // Create new session
        Session newSession = Session.builder()
                .terminalId(terminalId)
                .userId(userId)
                .openingFloat(DEFAULT_OPENING_FLOAT)
                .status("OPEN")
                .openedAt(LocalDateTime.now())
                .build();

        sessionRepository.save(newSession);

        log.info("Created new session {} for terminal {} and user {}",
                newSession.getId(), terminalId, userId);

        return newSession;
    }

    private boolean isSessionExpired(Session session) {
        long hoursOpen = ChronoUnit.HOURS.between(session.getOpenedAt(), LocalDateTime.now());
        return hoursOpen >= SESSION_LIFETIME_HOURS;
    }

    private Session createNewSession(Long terminalId, Long userId) {
        Session newSession = Session.builder()
                .terminalId(terminalId)
                .userId(userId)
                .openingFloat(DEFAULT_OPENING_FLOAT)
                .openedAt(LocalDateTime.now())
                .status("OPEN")
                .build();

        Session saved = sessionRepository.save(newSession);
        log.info("Created new session {} for terminal {}", saved.getId(), terminalId);
        return saved;
    }

    private Session rotateSession(Session oldSession, Long terminalId, Long userId) {
        oldSession.setStatus("CLOSED");
        oldSession.setClosedAt(LocalDateTime.now());
        oldSession.setClosingAmount(BigDecimal.ZERO);

        sessionRepository.saveAndFlush(oldSession);
        log.info("Closed old session {}", oldSession.getId());

        return createNewSession(terminalId, userId);
    }
}
