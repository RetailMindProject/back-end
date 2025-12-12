package com.example.back_end.modules.terminal.controller;

import com.example.back_end.modules.terminal.dto.TerminalDTO;
import com.example.back_end.modules.terminal.service.TerminalOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller for Terminal POS operations
 * Used by cashiers for daily operations
 */
@RestController
@RequestMapping("/api/terminal")
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalOperationService terminalOperationService;

    /**
     * Get all available terminals for selection
     * GET /api/terminal/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<TerminalDTO.TerminalInfo>> getAvailableTerminals() {
        List<TerminalDTO.TerminalInfo> terminals = terminalOperationService.getAvailableTerminals();
        return ResponseEntity.ok(terminals);
    }

    /**
     * Open a new cashier session
     * POST /api/terminal/session/open
     */
    @PostMapping("/session/open")
    public ResponseEntity<TerminalDTO.SessionResponse> openSession(
            @Valid @RequestBody TerminalDTO.OpenSessionRequest request) {
        TerminalDTO.SessionResponse response = terminalOperationService.openSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Close current session
     * POST /api/terminal/session/close
     * userId is passed as query param (in real app, this comes from JWT token)
     */
    @PostMapping("/session/close")
    public ResponseEntity<TerminalDTO.SessionResponse> closeSession(
            @RequestParam Long userId,
            @Valid @RequestBody TerminalDTO.CloseSessionRequest request) {
        TerminalDTO.SessionResponse response = terminalOperationService.closeSession(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current active session for user
     * GET /api/terminal/session/current
     */
    @GetMapping("/session/current")
    public ResponseEntity<TerminalDTO.SessionResponse> getCurrentSession(@RequestParam Long userId) {
        TerminalDTO.SessionResponse response = terminalOperationService.getCurrentSession(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/last-session-info")
    public ResponseEntity<TerminalDTO.LastSessionInfo> getLastSessionInfo(@RequestParam Long userId) {
        TerminalDTO.LastSessionInfo info = terminalOperationService.getLastSessionInfo(userId);
        return ResponseEntity.ok(info);
    }
}