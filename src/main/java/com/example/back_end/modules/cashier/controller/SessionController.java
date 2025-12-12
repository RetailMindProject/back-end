package com.example.back_end.modules.cashier.controller;
import com.example.back_end.modules.cashier.dto.CashierDetailsDTO;
import com.example.back_end.modules.cashier.dto.CloseSessionRequest;
import com.example.back_end.modules.cashier.dto.SessionCardDTO;
import com.example.back_end.modules.cashier.dto.SessionFilterDTO;
import com.example.back_end.modules.cashier.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * REST Controller for Session Management
 * Handles Sessions List Page and Cashier Details Page
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionService sessionService;
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

    /**
     * GET /api/sessions/{sessionId}
     * Get cashier details for a specific session
     * For Cashier Details Page
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<CashierDetailsDTO> getCashierDetails(
            @PathVariable Long sessionId
    ) {
        log.info("GET /api/sessions/{} - Fetching cashier details", sessionId);

        CashierDetailsDTO cashierDetails = sessionService.getCashierDetails(sessionId);

        return ResponseEntity.ok(cashierDetails);
    }

    /**
     * GET /api/sessions/active
     * Get all active sessions only
     */
    @GetMapping("/active")
    public ResponseEntity<List<SessionCardDTO>> getActiveSessions() {
        log.info("GET /api/sessions/active - Fetching active sessions");

        List<SessionCardDTO> sessions = sessionService.getActiveSessions();

        log.info("Returning {} active sessions", sessions.size());
        return ResponseEntity.ok(sessions);
    }

    /**
     * PUT /api/sessions/{sessionId}/close
     * Close a cashier session by ID
     * Updates the session status to CLOSED, sets closedAt = now, and optionally saves closingAmount
     */
    @PutMapping("/{sessionId}/close")
    public ResponseEntity<SessionCardDTO> closeSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody CloseSessionRequest request) {
        log.info("PUT /api/sessions/{}/close - Closing session with closing amount: {}", 
                sessionId, request.getClosingAmount());

        SessionCardDTO closedSession = sessionService.closeSession(sessionId, request);

        log.info("Session {} closed successfully", sessionId);
        return ResponseEntity.ok(closedSession);
    }
}