package com.example.back_end.modules.inventory.controller;

import com.example.back_end.modules.inventory.dto.TransferRequestDTO;
import com.example.back_end.modules.inventory.service.TransferRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/transfer-requests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransferRequestController {

    private final TransferRequestService transferRequestService;

    /**
     * Store Manager creates a request (PENDING). No inventory changes.
     */
    @PostMapping
    @PreAuthorize("hasRole('STORE_MANAGER')")
    public ResponseEntity<TransferRequestDTO.Response> create(
            @Valid @RequestBody TransferRequestDTO.CreateRequest request
    ) {
        TransferRequestDTO.Response resp = transferRequestService.createRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * Inventory Manager lists requests (optionally filtered by status).
     * Example: GET /api/inventory/transfer-requests?status=PENDING
     */
    @GetMapping
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<List<TransferRequestDTO.Response>> list(
            @RequestParam(value = "status", required = false) TransferRequestDTO.RequestStatus status
    ) {
        return ResponseEntity.ok(transferRequestService.list(status));
    }

    /**
     * Inventory Manager gets full details for one request.
     * Example: GET /api/inventory/transfer-requests/123
     */
    @GetMapping("/{requestId}")
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<TransferRequestDTO.Response> getById(@PathVariable Long requestId) {
        return ResponseEntity.ok(transferRequestService.getById(requestId));
    }

    /**
     * Inventory Manager approves and executes the transfer atomically.
     */
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<TransferRequestDTO.Response> approve(@PathVariable Long requestId) {
        return ResponseEntity.ok(transferRequestService.approve(requestId));
    }

    /**
     * Inventory Manager rejects request (no inventory changes).
     */
    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<TransferRequestDTO.Response> reject(
            @PathVariable Long requestId,
            @Valid @RequestBody TransferRequestDTO.RejectRequest request
    ) {
        return ResponseEntity.ok(transferRequestService.reject(requestId, request));
    }
}