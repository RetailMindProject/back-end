package com.example.back_end.modules.terminal.service;

import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.register.repository.UserRepository;
import com.example.back_end.modules.terminal.dto.PairingRequestDTO;
import com.example.back_end.modules.terminal.entity.Terminal;
import com.example.back_end.modules.terminal.entity.TerminalDevice;
import com.example.back_end.modules.terminal.entity.TerminalPairingCode;
import com.example.back_end.modules.terminal.repository.TerminalDeviceRepository;
import com.example.back_end.modules.terminal.repository.TerminalPairingCodeRepository;
import com.example.back_end.modules.terminal.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling pairing request-approval workflow
 * This is for the NEW flow where cashier requests and manager approves
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PairingRequestService {

    private final TerminalPairingCodeRepository pairingCodeRepository;
    private final TerminalDeviceRepository terminalDeviceRepository;
    private final TerminalRepository terminalRepository;
    private final UserRepository userRepository;

    private static final int REQUEST_EXPIRY_MINUTES = 5;

    /**
     * Flow 2: Cashier creates pairing request
     */
    @Transactional
    public PairingRequestDTO.Response  createPairingRequest(
            PairingRequestDTO.CreateRequest request,
            Long cashierUserId,
            String browserTokenHash) {

        log.info("Creating pairing request for cashier {} and terminal {}",
                cashierUserId, request.getTerminalId());

        // Validate terminal
        Terminal terminal = terminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found"));

        if (!terminal.getIsActive()) {
            throw new IllegalArgumentException("Terminal is not active");
        }

        // Check if terminal already has active request
        if (pairingCodeRepository.hasActiveRequest(request.getTerminalId(), LocalDateTime.now())) {
            throw new IllegalStateException(
                    "This terminal already has a pending pairing request. Please wait.");
        }

        // Check if cashier already has active request
        pairingCodeRepository.findActiveRequestByUser(cashierUserId, LocalDateTime.now())
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "You already have a pending pairing request. Please wait for approval.");
                });

        // Create request in terminal_pairing_codes
        TerminalPairingCode pairingRequest = TerminalPairingCode.builder()
                .terminalId(request.getTerminalId())
                .requestedBy(cashierUserId)
                .requestTokenHash(browserTokenHash)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(REQUEST_EXPIRY_MINUTES))
                .status("PENDING")
                .codeHash(null) // nullable - not used in this flow
                .build();

        pairingCodeRepository.save(pairingRequest);

        log.info("Created pairing request {} for terminal {}",
                pairingRequest.getId(), terminal.getCode());

        return mapToResponse(pairingRequest);
    }

    /**
     * Flow 3: Manager views pending requests
     */
    @Transactional
    public List<PairingRequestDTO.Response > getPendingRequests() {
        // Mark expired requests first
        pairingCodeRepository.markExpiredRequests(LocalDateTime.now());

        List<TerminalPairingCode> requests = pairingCodeRepository
                .findPendingRequests(LocalDateTime.now());

        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Flow 3 + 4: Manager approves request → Auto-pairing
     */
    @Transactional
    public PairingRequestDTO.Response approveRequest(Long requestId, Long managerId) {

        TerminalPairingCode request = pairingCodeRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Request is not pending");
        }

        if (request.isExpired()) {
            request.setStatus("EXPIRED");
            pairingCodeRepository.save(request);
            throw new IllegalStateException("Request has expired");
        }

        log.info("Approving pairing request {} for terminal {}", requestId, request.getTerminalId());

        // ✅ Flow 4.5A: Revoke old devices on same terminal
        List<TerminalDevice> oldDevicesForTerminal = terminalDeviceRepository
                .findByTerminalId(request.getTerminalId());

        if (!oldDevicesForTerminal.isEmpty()) {
            log.info("Revoking {} existing device(s) for terminal {}",
                    oldDevicesForTerminal.size(), request.getTerminalId());

            oldDevicesForTerminal.forEach(device -> {
                device.setRevokedAt(LocalDateTime.now());
                terminalDeviceRepository.save(device);
            });

            terminalDeviceRepository.flush();
        }

        // ✅✅ Flow 4.5B: DELETE old devices for same browser token (to avoid UNIQUE constraint)
        List<TerminalDevice> oldDevicesForBrowser = terminalDeviceRepository
                .findByTokenHash(request.getRequestTokenHash());

        if (!oldDevicesForBrowser.isEmpty()) {
            log.info("Deleting {} existing device(s) for this browser token",
                    oldDevicesForBrowser.size());
            terminalDeviceRepository.deleteAll(oldDevicesForBrowser);
            terminalDeviceRepository.flush();
        }

        // ✅ Flow 4.6: Create new device (auto-pairing)
        TerminalDevice newDevice = TerminalDevice.builder()
                .terminalId(request.getTerminalId())
                .tokenHash(request.getRequestTokenHash())
                .issuedAt(LocalDateTime.now())
                .lastSeenAt(LocalDateTime.now())
                .revokedAt(null)
                .build();

        terminalDeviceRepository.save(newDevice);

        // ✅ Flow 4.7: Mark request as USED
        request.setStatus("USED");
        request.setApprovedBy(managerId);
        request.setApprovedAt(LocalDateTime.now());
        request.setUsedAt(LocalDateTime.now());

        pairingCodeRepository.save(request);

        log.info("Successfully paired terminal {} with browser (request {})",
                request.getTerminalId(), requestId);

        return mapToResponse(request);
    }
    /**
     * Flow 3: Manager rejects request
     */
    @Transactional
    public PairingRequestDTO.Response  rejectRequest(
            Long requestId,
            PairingRequestDTO.RejectRequest rejectRequest,
            Long managerId) {

        TerminalPairingCode request = pairingCodeRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Request is not pending");
        }

        request.setStatus("REJECTED");
        request.setApprovedBy(managerId);
        request.setApprovedAt(LocalDateTime.now());

        pairingCodeRepository.save(request);

        log.info("Rejected pairing request {} for terminal {} - Reason: {}",
                requestId, request.getTerminalId(), rejectRequest.getReason());

        return mapToResponse(request);
    }

    /**
     * Flow 5: Cashier polls for request status
     */
    public PairingRequestDTO.Response  checkRequestStatus(
            Long terminalId,
            String browserTokenHash) {

        // Mark expired requests
        pairingCodeRepository.markExpiredRequests(LocalDateTime.now());

        TerminalPairingCode request = pairingCodeRepository
                .findLatestByTerminalAndToken(
                        terminalId,
                        browserTokenHash,
                        org.springframework.data.domain.PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElse(null);




        if (request == null) {
            return null;
        }

        // Auto-update status if expired
        if (request.isExpired() &&
                ("PENDING".equals(request.getStatus()) || "APPROVED".equals(request.getStatus()))) {
            request.setStatus("EXPIRED");
            pairingCodeRepository.save(request);
        }

        return mapToResponse(request);
    }

    /**
     * Map entity to DTO
     */
    private PairingRequestDTO.Response  mapToResponse(TerminalPairingCode request) {
        Terminal terminal = terminalRepository.findById(request.getTerminalId()).orElse(null);
        User requester = request.getRequestedBy() != null ?
                userRepository.findById(request.getRequestedBy().intValue()).orElse(null) : null;
        User approver = request.getApprovedBy() != null ?
                userRepository.findById(request.getApprovedBy().intValue()).orElse(null) : null;

        String message = switch (request.getStatus()) {
            case "PENDING" -> "Your order is pending - please wait until the store manager approves.";
            case "USED" -> "Connection successful - you can proceed";
            case "REJECTED" -> "Your request has been rejected - please contact the manager";
            case "EXPIRED" -> "Your order has expired - please try again.";
            default -> "Unknown case";
        };

        return PairingRequestDTO.Response .builder()
                .id(request.getId())
                .terminalId(request.getTerminalId())
                .terminalCode(terminal != null ? terminal.getCode() : null)
                .terminalDescription(terminal != null ? terminal.getDescription() : null)
                .requestedBy(request.getRequestedBy())
                .requestedByName(requester != null ?
                        requester.getFirstName() + " " + requester.getLastName() : "Unknown")
                .issuedAt(request.getIssuedAt())
                .expiresAt(request.getExpiresAt())
                .status(request.getStatus())
                .approvedBy(request.getApprovedBy())
                .approvedByName(approver != null ?
                        approver.getFirstName() + " " + approver.getLastName() : null)
                .approvedAt(request.getApprovedAt())
                .message(message)
                .build();
    }
}