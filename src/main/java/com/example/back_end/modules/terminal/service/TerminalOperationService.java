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
     * Get last session closing amount for cashier
     * Used to show cashier the previous closing amount before opening new session
     */
    @Transactional(readOnly = true)
    public TerminalDTO.LastSessionInfo getLastSessionInfo(Long userId) {
        // Get last closed session for this user
        List<Session> userSessions = sessionRepository.findByUserIdOrderByOpenedAtDesc(userId);

        // Find the last CLOSED session
        Optional<Session> lastClosedSession = userSessions.stream()
                .filter(s -> s.getStatus() == Session.SessionStatus.CLOSED)
                .findFirst();

        if (lastClosedSession.isEmpty()) {
            // First time for this cashier
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
     * Validates: terminal exists, terminal active, no existing session on terminal, no existing session for user
     */
    @Transactional
    public TerminalDTO.SessionResponse openSession(TerminalDTO.OpenSessionRequest request) {
        // Validate terminal
        Terminal terminal = terminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found with id: " + request.getTerminalId()));

        if (!terminal.getIsActive()) {
            throw new BusinessRuleException("Cannot open session on inactive terminal");
        }

        // Check if terminal already has active session
        Optional<Session> existingTerminalSession = sessionRepository.findOpenSessionByTerminalId(request.getTerminalId());
        if (existingTerminalSession.isPresent()) {
            throw new BusinessRuleException("Terminal already has an active session");
        }

        // Validate user
        User user = userRepository.findById(request.getUserId().intValue())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        // Check if user already has active session
        Optional<Session> existingUserSession = sessionRepository.findOpenSessionByUserId(request.getUserId().intValue());
        if (existingUserSession.isPresent()) {
            throw new BusinessRuleException("You already have an active session on another terminal");
        }

        // Create new session
        Session session = new Session();
        session.setTerminal(terminal);
        session.setUser(user);
        session.setOpeningFloat(request.getOpeningFloat());
        session.setOpenedAt(LocalDateTime.now());
        session.setStatus(Session.SessionStatus.OPEN);

        Session savedSession = sessionRepository.save(session);

        return buildSessionResponse(savedSession);
    }

    /**
     * Close current cashier session
     * Validates: session exists, session is open, no pending orders
     */
    @Transactional
    public TerminalDTO.SessionResponse closeSession(Long userId, TerminalDTO.CloseSessionRequest request) {
        // Get user's active session
        Session session = sessionRepository.findOpenSessionByUserId(userId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("No active session found for this user"));

        // TODO: Check for pending orders (DRAFT or HELD)
        // This will be implemented when sales module is complete

        // Close session
        session.setClosedAt(LocalDateTime.now());
        session.setClosingAmount(request.getClosingAmount());
        session.setStatus(Session.SessionStatus.CLOSED);

        Session updatedSession = sessionRepository.save(session);

        return buildSessionResponse(updatedSession);
    }

    /**
     * Get current active session for user
     */
    @Transactional(readOnly = true)
    public TerminalDTO.SessionResponse getCurrentSession(Long userId) {
        Session session = sessionRepository.findOpenSessionByUserId(userId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("No active session found for this user"));

        return buildSessionResponse(session);
    }

    /**
     * Helper method to build SessionResponse DTO
     */
    private TerminalDTO.SessionResponse buildSessionResponse(Session session) {
        return TerminalDTO.SessionResponse.builder()
                .sessionId(session.getId())
                .terminalId(session.getTerminal().getId())
                .terminalCode(session.getTerminal().getCode())
                .userId(session.getUser().getId().longValue())
                .userName(session.getUser().getFirstName() + " " + session.getUser().getLastName())
                .openedAt(session.getOpenedAt())
                .closedAt(session.getClosedAt())
                .openingFloat(session.getOpeningFloat())
                .closingAmount(session.getClosingAmount())
                .status(session.getStatus().name())
                .build();
    }
}