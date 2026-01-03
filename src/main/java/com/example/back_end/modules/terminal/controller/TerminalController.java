package com.example.back_end.modules.terminal.controller;

import com.example.back_end.common.dto.BrowserContext;
import com.example.back_end.common.filter.BrowserTokenFilter;
import com.example.back_end.exception.InvalidPairingCodeException;
import com.example.back_end.exception.NoTerminalPairedException;
import com.example.back_end.exception.TerminalAlreadyPairedException;
import com.example.back_end.modules.terminal.dto.*;
import com.example.back_end.modules.terminal.repository.TerminalDeviceRepository;
import com.example.back_end.modules.terminal.service.TerminalOperationService;
import com.example.back_end.modules.terminal.service.TerminalPairingService;
import com.example.back_end.modules.terminal.service.TerminalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Terminal operations
 * Handles both pairing operations and session management
 */
@RestController
@RequestMapping("/api/terminal")
@RequiredArgsConstructor
@Slf4j
public class TerminalController {

    private final TerminalPairingService pairingService;
    private final TerminalOperationService terminalOperationService;
    private final TerminalDeviceRepository terminalDeviceRepository;
    private final TerminalService terminalService;


    // ========================================
    // PAIRING OPERATIONS (NEW)
    // ========================================

    /**
     * Pair browser with a terminal using pairing code
     * POST /api/terminal/pair
     */
    @PostMapping("/pair")
    public ResponseEntity<?> pairTerminal(
            @Valid @RequestBody PairingRequestDTO request,
            HttpServletRequest httpRequest) {

        try {
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

            if (context == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Browser context not available"));
            }

            PairingResponseDTO response = pairingService.pairTerminal(
                    request, context.getBrowserTokenHash());

            return ResponseEntity.ok(response);

        } catch (TerminalAlreadyPairedException e) {
            // ✅ Special handling for already paired exception
            log.warn("Terminal already paired: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "TERMINAL_ALREADY_PAIRED");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("requiresConfirmation", true);

            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

        } catch (InvalidPairingCodeException e) {
            log.warn("Invalid pairing code: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Error pairing terminal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to pair terminal: " + e.getMessage()));
        }
    }

    /**
     * Switch to a different terminal
     * POST /api/terminal/switch
     */
    @PostMapping("/switch")
    public ResponseEntity<?> switchTerminal(
            @Valid @RequestBody SwitchTerminalRequestDTO request,
            HttpServletRequest httpRequest) {

        try {
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

            if (context == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Browser context not available"));
            }

            PairingResponseDTO response = pairingService.switchTerminal(
                    request, context.getBrowserTokenHash());

            return ResponseEntity.ok(response);

        } catch (NoTerminalPairedException e) {
            log.warn("No terminal paired: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));

        } catch (InvalidPairingCodeException e) {
            log.warn("Invalid pairing code: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));

        } catch (TerminalAlreadyPairedException e) {
            log.warn("Terminal already paired: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Unpair browser from current terminal
     * DELETE /api/terminal/unpair
     */
    @DeleteMapping("/unpair")
    @Transactional
    public ResponseEntity<?> unpairCurrentDevice(HttpServletRequest httpRequest) {
        try {
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

            if (context == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Browser context not available"));
            }

            if (!context.isPaired()) {
                return ResponseEntity.ok(Map.of(
                        "message", "Device is not paired with any terminal",
                        "isPaired", false
                ));
            }

            terminalDeviceRepository.findByTokenHashAndRevokedAtIsNull(context.getBrowserTokenHash())
                    .ifPresent(device -> {
                        log.info("Unpairing browser from terminal {}", device.getTerminalId());
                        device.setRevokedAt(LocalDateTime.now());
                        terminalDeviceRepository.save(device);
                    });

            return ResponseEntity.ok(Map.of(
                    "message", "Device unpaired successfully",
                    "isPaired", false
            ));

        } catch (Exception e) {
            log.error("Error unpairing device: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to unpair device"));
        }
    }

    /**
     * Get current terminal information
     * GET /api/terminal/current
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentTerminal(HttpServletRequest httpRequest) {
        try {
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

            if (context == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Browser context not available"));
            }

            if (!context.isPaired()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No terminal is paired with this browser"));
            }

            CurrentTerminalDTO response = pairingService.getCurrentTerminal(
                    context.getBrowserTokenHash());

            return ResponseEntity.ok(response);

        } catch (NoTerminalPairedException e) {
            log.warn("No terminal paired: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get pairing status (whether browser is paired)
     * GET /api/terminal/pairing-status
     */
    @GetMapping("/pairing-status")
    public ResponseEntity<?> getPairingStatus(HttpServletRequest httpRequest) {
        BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

        if (context == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Browser context not available"));
        }

        Map<String, Object> status = new HashMap<>();
        status.put("isPaired", context.isPaired());

        if (context.isPaired()) {
            status.put("terminalId", context.getTerminalId());

            try {
                CurrentTerminalDTO terminal = pairingService.getCurrentTerminal(
                        context.getBrowserTokenHash());
                status.put("terminalCode", terminal.getTerminalCode());
                status.put("terminalDescription", terminal.getTerminalDescription());
            } catch (Exception e) {
                log.warn("Error getting terminal details: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(status);
    }

    /**
     * Generate pairing code for a terminal
     * POST /api/terminal/pairing-code
     * CEO only
     */
    @PostMapping("/pairing-code")
    public ResponseEntity<?> generatePairingCode(
            @Valid @RequestBody GeneratePairingCodeRequestDTO request,
            Authentication authentication) {

        log.info("Auth = {}", authentication);

        try {
            // ✅ Handle null or anonymous authentication
            Long userId = null;
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                try {
                    userId = getUserIdFromAuth(authentication);
                } catch (Exception e) {
                    log.warn("Could not extract userId from auth: {}", e.getMessage());
                }
            }

            GeneratePairingCodeResponseDTO response = pairingService.generatePairingCode(
                    request, userId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error generating pairing code: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // TERMINAL SELECTION & INFO (EXISTING - UPDATED)
    // ========================================

    /**
     * Get all available terminals for selection
     * Used during pairing process to show list of terminals
     * GET /api/terminal/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<TerminalDTO.TerminalInfo>> getAvailableTerminals() {
        List<TerminalDTO.TerminalInfo> terminals = terminalOperationService.getAvailableTerminals();
        return ResponseEntity.ok(terminals);
    }

    /**
     * Get terminal by ID
     * GET /api/terminal/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTerminalById(@PathVariable Long id) {
        try {
            TerminalDTO.TerminalInfo terminal = terminalOperationService.getTerminalById(id);
            return ResponseEntity.ok(terminal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Terminal not found"));
        }
    }

    // ========================================
    // TERMINAL MANAGEMENT (STORE_MANAGER)
    // ========================================

    /**
     * Create a new terminal
     * POST /api/terminal/management
     * STORE_MANAGER only
     */
    @PostMapping("/management")
    @PreAuthorize("hasRole('STORE_MANAGER') or hasRole('CEO')")
    public ResponseEntity<?> createTerminal(
            @Valid @RequestBody TerminalManagementDTO.CreateRequest request) {
        try {
            log.info("Creating terminal with code: {}", request.getCode());
            TerminalManagementDTO.TerminalResponse response = terminalService.createTerminal(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating terminal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all terminals
     * GET /api/terminal/management
     * STORE_MANAGER only
     */
    @GetMapping("/management")
    @PreAuthorize("hasRole('STORE_MANAGER') or hasRole('CEO')")
    public ResponseEntity<List<TerminalManagementDTO.TerminalResponse>> getAllTerminals() {
        log.info("Fetching all terminals");
        List<TerminalManagementDTO.TerminalResponse> terminals = terminalService.getAllTerminals();
        return ResponseEntity.ok(terminals);
    }

    /**
     * Get terminal by ID (management)
     * GET /api/terminal/management/{id}
     * STORE_MANAGER only
     */
    @GetMapping("/management/{id}")
    @PreAuthorize("hasRole('STORE_MANAGER') or hasRole('CEO')")
    public ResponseEntity<?> getTerminalByIdManagement(@PathVariable Long id) {
        try {
            TerminalManagementDTO.TerminalResponse terminal = terminalService.getTerminalById(id);
            return ResponseEntity.ok(terminal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Terminal not found"));
        }
    }

    /**
     * Update terminal
     * PUT /api/terminal/management/{id}
     * STORE_MANAGER only
     */
    @PutMapping("/management/{id}")
    @PreAuthorize("hasRole('STORE_MANAGER') or hasRole('CEO')")
    public ResponseEntity<?> updateTerminal(
            @PathVariable Long id,
            @Valid @RequestBody TerminalManagementDTO.UpdateRequest request) {
        try {
            log.info("Updating terminal with ID: {}", id);
            TerminalManagementDTO.TerminalResponse response = terminalService.updateTerminal(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating terminal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete terminal (soft delete - deactivate)
     * DELETE /api/terminal/management/{id}
     * STORE_MANAGER only
     */
    @DeleteMapping("/management/{id}")
    @PreAuthorize("hasRole('STORE_MANAGER') or hasRole('CEO')")
    public ResponseEntity<?> deleteTerminal(@PathVariable Long id) {
        try {
            log.info("Deleting terminal with ID: {}", id);
            terminalService.deleteTerminal(id);
            return ResponseEntity.ok(Map.of("message", "Terminal deactivated successfully"));
        } catch (Exception e) {
            log.error("Error deleting terminal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Activate terminal (reactivate)
     * PUT /api/terminal/management/{id}/activate
     * STORE_MANAGER only
     */
    @PutMapping("/management/{id}/activate")
    @PreAuthorize("hasRole('STORE_MANAGER') or hasRole('CEO')")
    public ResponseEntity<?> activateTerminal(@PathVariable Long id) {
        try {
            log.info("Activating terminal with ID: {}", id);
            TerminalManagementDTO.TerminalResponse response = terminalService.activateTerminal(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error activating terminal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // SESSION OPERATIONS (EXISTING - DEPRECATED)
    // Note: These are kept for backward compatibility
    // New flow uses SessionController with BrowserContext
    // ========================================

    /**
     * Open a new cashier session (LEGACY)
     * @deprecated Use pairing flow instead. Session opens automatically on pairing.
     * POST /api/terminal/session/open
     */
    @Deprecated
    @PostMapping("/session/open")
    public ResponseEntity<?> openSession(
            @Valid @RequestBody TerminalDTO.OpenSessionRequest request,
            HttpServletRequest httpRequest) {

        BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

        // Check if browser is paired
        if (context == null || !context.isPaired()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Browser not paired with a terminal",
                            "message", "Please pair with a terminal first using /api/terminal/pair"
                    ));
        }

        // Verify terminal ID matches paired terminal
        if (!request.getTerminalId().equals(context.getTerminalId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Terminal ID mismatch",
                            "message", "You are paired with terminal " + context.getTerminalId() +
                                    " but trying to open session for terminal " + request.getTerminalId()
                    ));
        }

        try {
            TerminalDTO.SessionResponse response = terminalOperationService.openSession(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Close current session (LEGACY)
     * @deprecated Use /api/cashier/session/close instead
     * POST /api/terminal/session/close
     */
    @Deprecated
    @PostMapping("/session/close")
    public ResponseEntity<?> closeSession(
            @RequestParam Long userId,
            @Valid @RequestBody TerminalDTO.CloseSessionRequest request,
            HttpServletRequest httpRequest) {

        BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

        if (context == null || !context.isPaired()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Browser not paired with a terminal"));
        }

        try {
            TerminalDTO.SessionResponse response = terminalOperationService.closeSession(userId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current active session for user (LEGACY)
     * @deprecated Use /api/cashier/session/current instead
     * GET /api/terminal/session/current
     */
    @Deprecated
    @GetMapping("/session/current")
    public ResponseEntity<?> getCurrentSession(
            @RequestParam Long userId,
            HttpServletRequest httpRequest) {

        BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

        if (context == null || !context.isPaired()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Browser not paired with a terminal"));
        }

        try {
            TerminalDTO.SessionResponse response = terminalOperationService.getCurrentSession(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No active session found"));
        }
    }

    /**
     * Get last session info (LEGACY)
     * GET /api/terminal/last-session-info
     */
    @GetMapping("/last-session-info")
    public ResponseEntity<?> getLastSessionInfo(
            @RequestParam Long userId,
            HttpServletRequest httpRequest) {

        BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

        if (context == null || !context.isPaired()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Browser not paired with a terminal"));
        }

        try {
            TerminalDTO.LastSessionInfo info = terminalOperationService.getLastSessionInfo(userId);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No session info found"));
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Helper to extract user ID from authentication
     * Implement based on your authentication setup
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        // TODO: Implement based on your UserDetails or JWT claims
        // For now, return a placeholder
        // Example:
        // UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // return ((CustomUserDetails) userDetails).getUserId();
        return 1L; // Replace with actual implementation
    }
}