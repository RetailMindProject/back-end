package com.example.back_end.modules.terminal.service;

import com.example.back_end.exception.BusinessRuleException;
import com.example.back_end.exception.ResourceNotFoundException;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.cashier.repository.SessionRepository;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.repository.UserRepository;
import com.example.back_end.modules.terminal.dto.TerminalDTO;
import com.example.back_end.modules.terminal.entity.Terminal;
import com.example.back_end.modules.terminal.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Terminal POS operations
 * Handles cashier daily operations: open/close session, terminal selection
 */
@Service
@RequiredArgsConstructor
public class TerminalOperationService {

    private final TerminalRepository terminalRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private static final BigDecimal DEFAULT_OPENING_FLOAT = new BigDecimal("2000.00");

    /**
     * Get all available terminals for selection
     * Shows which terminals have active sessions
     */
    @Transactional(readOnly = true)
    public List<TerminalDTO.TerminalInfo> getAvailableTerminals() {
        List<Terminal> terminals = terminalRepository.findByIsActiveTrue();

        return terminals.stream().map(terminal -> {
            Optional<Session> activeSession = sessionRepository.findOpenSessionByTerminalId(terminal.getId());

            return TerminalDTO.TerminalInfo.builder()
                    .id(terminal.getId())
                    .code(terminal.getCode())
                    .description(terminal.getDescription())
                    .isActive(terminal.getIsActive())
                    .hasActiveSession(activeSession.isPresent())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Get terminal by ID
     */
    @Transactional(readOnly = true)
    public TerminalDTO.TerminalInfo getTerminalById(Long id) {
        Terminal terminal = terminalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found with id: " + id));

        Optional<Session> activeSession = sessionRepository.findOpenSessionByTerminalId(terminal.getId());

        return TerminalDTO.TerminalInfo.builder()
                .id(terminal.getId())
                .code(terminal.getCode())
                .description(terminal.getDescription())
                .isActive(terminal.getIsActive())
                .hasActiveSession(activeSession.isPresent())
                .build();
    }

    /**
     * Get last session closing amount for cashier
     * Used to show cashier the previous closing amount before opening new session
     */
    @Transactional(readOnly = true)
    public TerminalDTO.LastSessionInfo getLastSessionInfo(Long userId) {
        List<Session> userSessions = sessionRepository.findByUserIdOrderByOpenedAtDesc(userId);

        Optional<Session> lastClosedSession = userSessions.stream()
                .filter(s -> "CLOSED".equals(s.getStatus()))
                .findFirst();

        if (lastClosedSession.isEmpty()) {
            return TerminalDTO.LastSessionInfo.builder()
                    .message("No previous session found. This is your first session.")
                    .build();
        }

        Session lastSession = lastClosedSession.get();

        return TerminalDTO.LastSessionInfo.builder()
                .lastSessionId(lastSession.getId())
                .closedAt(lastSession.getClosedAt())
                .closingAmount(lastSession.getClosingAmount())
                .terminalCode(lastSession.getTerminal().getCode())
                .message("Your last session closed with: " + lastSession.getClosingAmount())
                .build();
    }

    /**
     * Open a new cashier session on selected terminal
     * @deprecated Session now opens automatically on terminal pairing
     */
    @Deprecated
    @Transactional
    public TerminalDTO.SessionResponse openSession(TerminalDTO.OpenSessionRequest request) {

        Terminal terminal = terminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found with id: " + request.getTerminalId()));

        if (!terminal.getIsActive()) {
            throw new BusinessRuleException("Cannot open session on inactive terminal");
        }

        // ✅ Check OPEN session on THIS terminal only
        Optional<Session> existingTerminalSession =
                sessionRepository.findOpenSessionByTerminalId(request.getTerminalId());

        if (existingTerminalSession.isPresent()) {
            Session s = existingTerminalSession.get();

            // (Optional) keep session user aligned with current request user
            if (request.getUserId() != null && (s.getUserId() == null || !request.getUserId().equals(s.getUserId()))) {
                s.setUserId(request.getUserId());
                s = sessionRepository.save(s);
            }

            // (Optional) if openingFloat is null, keep existing; otherwise ignore (openingFloat should not change for open session)
            return buildSessionResponse(s);
        }

        // ✅ Only validate user exists (no "user has another open session" check)
        userRepository.findById(request.getUserId().intValue())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Session session = Session.builder()
                .terminalId(request.getTerminalId())
                .userId(request.getUserId())
                .openingFloat(request.getOpeningFloat() != null ? request.getOpeningFloat() : DEFAULT_OPENING_FLOAT)
                .openedAt(LocalDateTime.now())
                .status("OPEN")
                .build();

        Session savedSession = sessionRepository.save(session);

        return buildSessionResponse(savedSession);
    }

    /**
     * Close current cashier session
     * @deprecated Use SessionLifecycleService for session management
     */
    @Deprecated
    @Transactional
    public TerminalDTO.SessionResponse closeSession(Long userId, TerminalDTO.CloseSessionRequest request) {

        Session session = sessionRepository.findOpenSessionByTerminalId(request.getTerminalId())
                .orElseThrow(() -> new ResourceNotFoundException("No active session found for this terminal"));

        session.setClosedAt(LocalDateTime.now());
        session.setClosingAmount(request.getClosingAmount());
        session.setStatus("CLOSED");

        Session updatedSession = sessionRepository.save(session);

        return buildSessionResponse(updatedSession);
    }


    /**
     * Get current active session for user
     * @deprecated Use SessionController endpoints with BrowserContext
     */
    @Deprecated
    @Transactional(readOnly = true)
    public TerminalDTO.SessionResponse getCurrentSession(Long terminalId) {

        Session session = sessionRepository.findOpenSessionByTerminalId(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("No active session found for this terminal"));

        return buildSessionResponse(session);
    }


    /**
     * Helper method to build SessionResponse DTO
     */
    private TerminalDTO.SessionResponse buildSessionResponse(Session session) {
        return TerminalDTO.SessionResponse.builder()
                .sessionId(session.getId())
                .terminalId(session.getTerminal() != null ? session.getTerminal().getId() : session.getTerminalId())
                .terminalCode(session.getTerminal() != null ? session.getTerminal().getCode() : null)
                .userId(session.getUser() != null ? session.getUser().getId().longValue() : session.getUserId())
                .userName(session.getUser() != null ?
                        session.getUser().getFirstName() + " " + session.getUser().getLastName() : null)
                .openedAt(session.getOpenedAt())
                .closedAt(session.getClosedAt())
                .openingFloat(session.getOpeningFloat())
                .closingAmount(session.getClosingAmount())
                .status(session.getStatus())  // ← شلت .name() لأن status صار String
                .build();
    }
}