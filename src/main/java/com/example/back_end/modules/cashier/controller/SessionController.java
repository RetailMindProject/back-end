package com.example.back_end.modules.cashier.controller;

import com.example.back_end.common.dto.BrowserContext;
import com.example.back_end.common.filter.BrowserTokenFilter;
import com.example.back_end.modules.cashier.dto.*;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.cashier.repository.SessionRepository;
import com.example.back_end.modules.cashier.service.SessionLifecycleService;
import com.example.back_end.modules.cashier.service.SessionService;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.repository.UserRepository;
import com.example.back_end.modules.terminal.dto.CurrentTerminalDTO;
import com.example.back_end.modules.terminal.entity.Terminal;
import com.example.back_end.modules.terminal.repository.TerminalRepository;
import com.example.back_end.modules.terminal.service.TerminalPairingService;
import com.example.back_end.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionService sessionService;
    private final SessionLifecycleService lifecycleService;
    private final TerminalPairingService pairingService;
    private final TerminalRepository terminalRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRepository sessionRepository;  // ← أضف هذا
    private final JwtService jwtService;


    // ========================================
    // SESSION LIST & DETAILS (EXISTING)
    // ========================================

    @GetMapping
    public ResponseEntity<List<SessionCardDTO>> getAllSessions(
            @RequestParam(required = false) String cashierName,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalTime time,
            @RequestParam(required = false) String status
    ) {
        log.info("GET /api/sessions - cashierName: {}, date: {}, time: {}, status: {}",
                cashierName, date, time, status);

        SessionFilterDTO filter = SessionFilterDTO.builder()
                .cashierName(cashierName)
                .date(date)
                .time(time)
                .status(status)
                .build();

        List<SessionCardDTO> sessions = sessionService.getAllSessions(filter);

        log.info("Returning {} sessions", sessions.size());
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<CashierDetailsDTO> getCashierDetails(
            @PathVariable Long sessionId
    ) {
        log.info("GET /api/sessions/{} - Fetching cashier details", sessionId);

        CashierDetailsDTO cashierDetails = sessionService.getCashierDetails(sessionId);

        return ResponseEntity.ok(cashierDetails);
    }

    @GetMapping("/active")
    public ResponseEntity<List<SessionCardDTO>> getActiveSessions() {
        log.info("GET /api/sessions/active - Fetching active sessions");

        List<SessionCardDTO> sessions = sessionService.getActiveSessions();

        log.info("Returning {} active sessions", sessions.size());
        return ResponseEntity.ok(sessions);
    }

    @PutMapping("/{sessionId}/close")
    public ResponseEntity<SessionCardDTO> closeSessionById(
            @PathVariable Long sessionId,
            @Valid @RequestBody CloseSessionRequest request) {
        log.info("PUT /api/sessions/{}/close - Closing session with closing amount: {}",
                sessionId, request.getClosingAmount());

        SessionCardDTO closedSession = sessionService.closeSession(sessionId, request);

        log.info("Session {} closed successfully", sessionId);
        return ResponseEntity.ok(closedSession);
    }

    // ========================================
    // CASHIER OPERATIONS (NEW)
    // ========================================

    @PostMapping("/cashier/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest) {

        try {
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

            if (context == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Browser context not available"));
            }

            // Authenticate user
            User user = authenticateUser(request.getUsername(), request.getPassword());

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }

            // Generate JWT token
            String jwtToken = jwtService.generateToken(user.getEmail(), user.getRole().name());

            LoginResponseDTO.LoginResponseDTOBuilder responseBuilder = LoginResponseDTO.builder()
                    .userId(user.getId().longValue())
                    .username(user.getEmail())
                    .role(user.getRole().name())
                    .token(jwtToken)
                    .isPaired(context.isPaired());

            // ✅ Only proceed if paired
            if (!context.isPaired()) {
                responseBuilder.message("Login successful. Please pair with a terminal to start.");
                return ResponseEntity.ok(responseBuilder.build());
            }

            try {
                CurrentTerminalDTO terminal = pairingService.getCurrentTerminal(
                        context.getBrowserTokenHash());

                // ✅ Check if another user has open session on THIS terminal
                Optional<Session> existingSession = sessionRepository
                        .findOpenSessionByTerminalId(terminal.getTerminalId());

                if (existingSession.isPresent()) {
                    Session session = existingSession.get();

                    // ✅ Check if different user
                    if (session.getUserId() != null &&
                            !session.getUserId().equals(user.getId().longValue())) {

                        // ❌ Different user - must close old session first
                        User currentUser = userRepository.findById(session.getUserId().intValue())
                                .orElse(null);

                        String currentUserName = currentUser != null ?
                                currentUser.getFirstName() + " " + currentUser.getLastName() :
                                "Unknown user";

                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "ANOTHER_USER_ACTIVE");
                        errorResponse.put("message", "User '" + currentUserName + "' is currently logged in on this terminal. " +
                                "Please ask them to close their session first.");
                        errorResponse.put("currentUserId", session.getUserId());
                        errorResponse.put("currentUserName", currentUserName);
                        errorResponse.put("sessionId", session.getId());

                        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                    }
                }

                // ✅ Ensure session exists or create new one
                Session session = lifecycleService.ensureSessionForLogin(
                        terminal.getTerminalId(),
                        user.getId().longValue()
                );

                responseBuilder
                        .terminalId(terminal.getTerminalId())
                        .terminalCode(terminal.getTerminalCode())
                        .sessionId(session.getId())
                        .sessionStatus(session.getStatus())
                        .openingFloat(session.getOpeningFloat().doubleValue())
                        .message("Welcome! You are paired with terminal " + terminal.getTerminalCode());

            } catch (Exception e) {
                log.error("Error getting terminal/session info: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to initialize session: " + e.getMessage()));
            }

            return ResponseEntity.ok(responseBuilder.build());

        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed: " + e.getMessage()));
        }
    }

    @GetMapping("/cashier/session/current")
    public ResponseEntity<?> getCurrentSession(HttpServletRequest httpRequest) {
        try {
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

            if (context == null || !context.isPaired()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No terminal is paired with this browser"));
            }

            // ✅ رجعناها لـ ensureValidSession
            Session session = lifecycleService.getCurrentSession(context.getTerminalId());
            if (session == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("hasOpenSession", false, "message", "No open session"));
            }

            Terminal terminal = terminalRepository.findById(context.getTerminalId())
                    .orElseThrow(() -> new IllegalStateException("Terminal not found"));

            String userName = null;
            if (session.getUserId() != null) {
                User user = userRepository.findById(session.getUserId().intValue()).orElse(null);
                if (user != null) {
                    userName = user.getFirstName() + " " + user.getLastName();
                }
            }

            SessionInfoDTO response = SessionInfoDTO.builder()
                    .sessionId(session.getId())
                    .status(session.getStatus())
                    .openedAt(session.getOpenedAt())
                    .openingFloat(session.getOpeningFloat().doubleValue())
                    .userId(session.getUserId())
                    .userName(userName)
                    .terminalId(terminal.getId())
                    .terminalCode(terminal.getCode())
                    .terminalDescription(terminal.getDescription())
                    .isPaired(true)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting current session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get session info"));
        }
    }

    @PostMapping("/cashier/session/close")
    public ResponseEntity<?> closeCurrentSession(
            @RequestBody CloseSessionRequestDTO request,
            HttpServletRequest httpRequest) {

        try {
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

            if (context == null || !context.isPaired()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No terminal is paired with this browser"));
            }

            // Get current session
            Session currentSession = lifecycleService.getCurrentSession(context.getTerminalId());

            if (currentSession == null || !"OPEN".equals(currentSession.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No open session found to close"));
            }

            // Close current session
            CloseSessionRequest closeRequest = CloseSessionRequest.builder()
                    .closingAmount(request.getClosingAmount() != null ?
                            BigDecimal.valueOf(request.getClosingAmount()) : null)
                    .build();

            sessionService.closeSession(currentSession.getId(), closeRequest);

            // ✅ No new session - just close and return
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Session closed successfully");
            response.put("closedSessionId", currentSession.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error closing session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to close session: " + e.getMessage()));
        }
    }

    @GetMapping("/cashier/session/status")
    public ResponseEntity<?> getSessionStatus(HttpServletRequest httpRequest) {
        try {
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

            if (context == null || !context.isPaired()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No terminal is paired with this browser"));
            }

            Session session = lifecycleService.getCurrentSession(context.getTerminalId());
            if (session == null) {
                return ResponseEntity.ok(Map.of(
                        "hasOpenSession", false,
                        "status", "NO_OPEN_SESSION"
                ));
            }

            Terminal terminal = terminalRepository.findById(context.getTerminalId())
                    .orElseThrow(() -> new IllegalStateException("Terminal not found"));

            long hoursOpen = ChronoUnit.HOURS.between(
                    session.getOpenedAt(), LocalDateTime.now());
            boolean needsRotation = hoursOpen >= 24;

            SessionStatusDTO response = SessionStatusDTO.builder()
                    .sessionId(session.getId())
                    .status(session.getStatus())
                    .openedAt(session.getOpenedAt())
                    .closedAt(session.getClosedAt())
                    .openingFloat(session.getOpeningFloat().doubleValue())
                    .closingAmount(session.getClosingAmount() != null ?
                            session.getClosingAmount().doubleValue() : null)
                    .terminalId(terminal.getId())
                    .terminalCode(terminal.getCode())
                    .terminalDescription(terminal.getDescription())
                    .hoursOpen(hoursOpen)
                    .needsRotation(needsRotation)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting session status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get session status"));
        }
    }
    /**
     * Authenticate user from database
     */
    private User authenticateUser(String username, String password) {
        try {
            User user = userRepository.findByEmail(username).orElse(null);

            if (user == null) {
                log.warn("User not found: {}", username);
                return null;
            }

            if (!Boolean.TRUE.equals(user.getIsActive())) {
                log.warn("User is not active: {}", username);
                return null;
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
                log.warn("Invalid password for user: {}", username);
                return null;
            }

            if (user.getRole() != User.UserRole.CASHIER) {
                log.warn("User is not a cashier: {}", username);
                return null;
            }

            log.info("User authenticated successfully: {}", username);
            return user;

        } catch (Exception e) {
            log.error("Error authenticating user: {}", e.getMessage(), e);
            return null;
        }
    }


}


