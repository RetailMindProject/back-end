package com.example.back_end.modules.terminal.controller;

import com.example.back_end.common.dto.BrowserContext;
import com.example.back_end.common.filter.BrowserTokenFilter;
import com.example.back_end.modules.terminal.dto.PairingRequestDTO;
import com.example.back_end.modules.terminal.service.PairingRequestService;
import com.example.back_end.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pairing-requests")
@RequiredArgsConstructor
@Slf4j
public class PairingRequestController {

    private final PairingRequestService pairingRequestService;
    private final JwtService jwtService;

    /**
     * Flow 2: Cashier creates pairing request
     * Browser-token only (NO JWT).
     */
    @PostMapping
    public ResponseEntity<?> createRequest(
            @Valid @RequestBody PairingRequestDTO.CreateRequest request,
            HttpServletRequest httpRequest) {
        try {
            // Get browser token
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);
            if (context == null) {
                return ResponseEntity.status(500)
                        .body(Map.of("error", "Browser context not available"));
            }

            // Cashier is not authenticated here => requestedBy is NULL
            PairingRequestDTO.Response response = pairingRequestService
                    .createPairingRequest(request, null, context.getBrowserTokenHash());

            return ResponseEntity.ok(response);

        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(409)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating pairing request: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to create pairing request"));
        }
    }

    /**
     * Flow 5: Cashier checks request status (polling)
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkStatus(
            @RequestParam Long terminalId,
            HttpServletRequest httpRequest) {
        try {
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);
            if (context == null) {
                return ResponseEntity.status(500)
                        .body(Map.of("error", "Browser context not available"));
            }

            PairingRequestDTO.Response response = pairingRequestService
                    .checkRequestStatus(terminalId, context.getBrowserTokenHash());

            if (response == null) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "No pairing request found"));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking request status: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to check request status"));
        }
    }

    /**
     * Flow 3: Manager views pending requests
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'CEO')")
    public ResponseEntity<?> getPendingRequests() {
        try {
            List<PairingRequestDTO.Response> requests = pairingRequestService
                    .getPendingRequests();

            return ResponseEntity.ok(requests);

        } catch (Exception e) {
            log.error("Error getting pending requests: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get pending requests"));
        }
    }

    /**
     * Flow 3 + 4: Manager approves request (auto-pairing)
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'CEO')")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        try {
            String token = extractToken(httpRequest);
            if (token == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "No token provided"));
            }

            Long managerId = jwtService.extractUserId(token);

            PairingRequestDTO.Response response = pairingRequestService
                    .approveRequest(id, managerId);

            return ResponseEntity.ok(response);

        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error approving request: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to approve request"));
        }
    }

    /**
     * Flow 3: Manager rejects request
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'CEO')")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody PairingRequestDTO.RejectRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = extractToken(httpRequest);
            if (token == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "No token provided"));
            }

            Long managerId = jwtService.extractUserId(token);

            PairingRequestDTO.Response response = pairingRequestService
                    .rejectRequest(id, request, managerId);

            return ResponseEntity.ok(response);

        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error rejecting request: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to reject request"));
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}