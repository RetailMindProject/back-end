package com.example.back_end.modules.terminal.service;

import com.example.back_end.common.util.HashUtil;
import com.example.back_end.exception.InvalidPairingCodeException;
import com.example.back_end.exception.NoTerminalPairedException;
import com.example.back_end.exception.TerminalAlreadyPairedException;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.cashier.repository.SessionRepository;
import com.example.back_end.modules.cashier.service.SessionLifecycleService;
import com.example.back_end.modules.terminal.dto.*;
import com.example.back_end.modules.terminal.entity.Terminal;
import com.example.back_end.modules.terminal.entity.TerminalDevice;
import com.example.back_end.modules.terminal.entity.TerminalPairingCode;
import com.example.back_end.modules.terminal.repository.TerminalDeviceRepository;
import com.example.back_end.modules.terminal.repository.TerminalPairingCodeRepository;
import com.example.back_end.modules.terminal.repository.TerminalRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling terminal pairing operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TerminalPairingService {

    private final TerminalRepository terminalRepository;
    private final TerminalDeviceRepository terminalDeviceRepository;
    private final TerminalPairingCodeRepository pairingCodeRepository;
    private final SessionRepository sessionRepository;
    private final SessionLifecycleService sessionLifecycleService;

    private static final Double DEFAULT_OPENING_FLOAT = 2000.0;

    /**
     * Pair a browser with a terminal
     * @param request Pairing request
     * @param browserTokenHash Hashed browser token
     * @return Pairing response
     */
    @Transactional
    public PairingResponseDTO pairTerminal(PairingRequestDTO request, String browserTokenHash) {
        log.info("Starting pairing process for terminal {} with token hash {}...",
                request.getTerminalId(), browserTokenHash.substring(0, 10));

        // Validate terminal
        Terminal terminal = terminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found"));

        if (!terminal.getIsActive()) {
            throw new IllegalArgumentException("Terminal is not active");
        }

        // Validate pairing code
        String codeHash = HashUtil.hashString(request.getPairingCode());
        TerminalPairingCode pairingCode = pairingCodeRepository
                .findValidByCodeHash(codeHash, LocalDateTime.now())
                .orElseThrow(() -> new InvalidPairingCodeException("Invalid or expired pairing code"));

        if (!pairingCode.getTerminalId().equals(request.getTerminalId())) {
            throw new InvalidPairingCodeException("Pairing code does not match the terminal");
        }

        // ✅ Check if terminal already paired with another browser
        Optional<TerminalDevice> existingPairing = terminalDeviceRepository
                .findByTerminalIdAndRevokedAtIsNull(request.getTerminalId());

        if (existingPairing.isPresent()) {
            TerminalDevice existingDevice = existingPairing.get();

            // ✅ Check if it's a different browser
            if (!existingDevice.getTokenHash().equals(browserTokenHash)) {

                // ✅ If forceOverride not explicitly set to true, throw exception
                if (request.getForceOverride() == null || !request.getForceOverride()) {
                    throw new TerminalAlreadyPairedException(
                            "Terminal is already paired with another browser. " +
                                    "Set forceOverride=true to override the existing pairing."
                    );
                }

                // ✅ User confirmed - revoke old pairing
                log.info("Force override - revoking existing pairing for terminal {}", request.getTerminalId());
                existingDevice.setRevokedAt(LocalDateTime.now());
                terminalDeviceRepository.save(existingDevice);
                terminalDeviceRepository.flush();
            }
        }

        // ✅ DELETE (not just revoke) any existing pairing for this browser
        // This includes BOTH active AND revoked records to avoid unique constraint
        List<TerminalDevice> existingDevicesForBrowser =
                terminalDeviceRepository.findByTokenHash(browserTokenHash);

        if (!existingDevicesForBrowser.isEmpty()) {
            log.info("Deleting {} existing pairing(s) for this browser", existingDevicesForBrowser.size());
            terminalDeviceRepository.deleteAll(existingDevicesForBrowser);
            terminalDeviceRepository.flush();
        }

        // Create new pairing
        TerminalDevice newDevice = TerminalDevice.builder()
                .terminalId(request.getTerminalId())
                .tokenHash(browserTokenHash)
                .issuedAt(LocalDateTime.now())
                .lastSeenAt(LocalDateTime.now())
                .build();

        terminalDeviceRepository.save(newDevice);

        // Mark pairing code as used
        pairingCode.setUsedAt(LocalDateTime.now());
        pairingCodeRepository.save(pairingCode);

        // Get session
        Session session = sessionLifecycleService.getCurrentSession(request.getTerminalId());

        log.info("Successfully paired terminal {} with browser", terminal.getCode());

        return PairingResponseDTO.builder()
                .terminalId(terminal.getId())
                .terminalCode(terminal.getCode())
                .terminalDescription(terminal.getDescription())
                .sessionId(session != null ? session.getId() : 0L)  // ✅ 0 بدل null
                .sessionStatus(session != null ? session.getStatus() : "NO_OPEN_SESSION")
                .openingFloat(session != null && session.getOpeningFloat() != null ?
                        session.getOpeningFloat().doubleValue() : 0.0)  // ✅ 0.0 بدل null
                .message(session != null
                        ? "Successfully paired with terminal " + terminal.getCode()
                        : "Paired successfully with terminal " + terminal.getCode() +
                        ". No open session - please start a new session.")
                .build();
    }
    /**
     * Switch to a different terminal
     * @param request Switch request
     * @param browserTokenHash Hashed browser token
     * @return Pairing response
     */
    @Transactional
    public PairingResponseDTO switchTerminal(SwitchTerminalRequestDTO request, String browserTokenHash) {
        log.info("Switching browser to terminal {}", request.getNewTerminalId());

        // 1. Revoke current pairing
        TerminalDevice currentDevice = terminalDeviceRepository.findByTokenHashAndRevokedAtIsNull(browserTokenHash)
                .orElseThrow(() -> new NoTerminalPairedException("No terminal is currently paired"));

        currentDevice.setRevokedAt(LocalDateTime.now());
        terminalDeviceRepository.save(currentDevice);

        // 2. Perform new pairing
        PairingRequestDTO pairingRequest = PairingRequestDTO.builder()
                .terminalId(request.getNewTerminalId())
                .pairingCode(request.getPairingCode())
                .build();

        return pairTerminal(pairingRequest, browserTokenHash);
    }

    /**
     * Unpair browser from terminal
     * @param browserTokenHash Hashed browser token
     */
    @Transactional
    public void unpairTerminal(String browserTokenHash) {
        log.info("Unpairing browser with token hash {}", browserTokenHash.substring(0, 10) + "...");

        TerminalDevice device = terminalDeviceRepository.findByTokenHashAndRevokedAtIsNull(browserTokenHash)
                .orElseThrow(() -> new NoTerminalPairedException("No terminal is currently paired"));

        Long terminalId = device.getTerminalId();

        // ✅ Soft delete (revoke)
        device.setRevokedAt(LocalDateTime.now());
        terminalDeviceRepository.save(device);

        log.info("Successfully unpaired browser from terminal {}", terminalId);
    }

    /**
     * Get current terminal information
     * @param browserTokenHash Hashed browser token
     * @return Current terminal info
     */
    public CurrentTerminalDTO getCurrentTerminal(String browserTokenHash) {
        TerminalDevice device = terminalDeviceRepository.findByTokenHashAndRevokedAtIsNull(browserTokenHash)
                .orElseThrow(() -> new NoTerminalPairedException("No terminal is currently paired"));

        Terminal terminal = terminalRepository.findById(device.getTerminalId())
                .orElseThrow(() -> new IllegalStateException("Terminal not found"));

        // Get current open session
        Session currentSession = sessionRepository.findOpenSessionByTerminalId(device.getTerminalId())
                .orElse(null);

        CurrentTerminalDTO.CurrentTerminalDTOBuilder builder = CurrentTerminalDTO.builder()
                .terminalId(terminal.getId())
                .terminalCode(terminal.getCode())
                .terminalDescription(terminal.getDescription())
                .isActive(terminal.getIsActive())
                .pairedAt(device.getIssuedAt())
                .lastSeenAt(device.getLastSeenAt());

        if (currentSession != null) {
            builder.currentSessionId(currentSession.getId())
                    .sessionStatus(currentSession.getStatus())
                    .sessionOpenedAt(currentSession.getOpenedAt())
                    .openingFloat(currentSession.getOpeningFloat().doubleValue());
        }

        return builder.build();
    }

    /**
     * Generate a new pairing code for a terminal
     * @param request Generation request
     * @param issuedByUserId User ID who issued the code
     * @return Generated code response
     */
    @Transactional
    public GeneratePairingCodeResponseDTO generatePairingCode(
            GeneratePairingCodeRequestDTO request,
            Long issuedByUserId) {

        log.info("Generating pairing code for terminal {}", request.getTerminalId());

        Terminal terminal = terminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found"));

        // ✅ Mark all existing active codes as used before generating new one
        pairingCodeRepository.markAllAsUsedForTerminal(request.getTerminalId(), LocalDateTime.now());

        // Generate plain code
        String plainCode = HashUtil.generatePairingCode();
        String codeHash = HashUtil.hashString(plainCode);

        // Calculate expiry
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusMinutes(request.getValidityMinutes());

        // Create pairing code
        TerminalPairingCode pairingCode = TerminalPairingCode.builder()
                .terminalId(request.getTerminalId())
                .codeHash(codeHash)
                .issuedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .issuedBy(issuedByUserId)
                .build();

        pairingCodeRepository.save(pairingCode);

        log.info("Generated pairing code for terminal {} (expires at {})",
                terminal.getCode(), expiresAt);

        return GeneratePairingCodeResponseDTO.builder()
                .terminalId(terminal.getId())
                .terminalCode(terminal.getCode())
                .pairingCode(plainCode)
                .expiresAt(expiresAt)
                .validityMinutes(request.getValidityMinutes())
                .message("Pairing code generated successfully. This code will expire in " +
                        request.getValidityMinutes() + " minutes.")
                .build();
    }

    /**
     * Open a session if no open session exists
     * @param terminalId Terminal ID
     * @return Open session (existing or newly created)
     */
    private Session openSessionIfNeeded(Long terminalId) {
        return sessionRepository.findOpenSessionByTerminalId(terminalId)
                .orElseGet(() -> {
                    log.info("No open session found for terminal {}. Creating new session.", terminalId);

                    Session newSession = Session.builder()
                            .terminalId(terminalId)
                            .openedAt(LocalDateTime.now())
                            .openingFloat(BigDecimal.valueOf(DEFAULT_OPENING_FLOAT))
                            .status("OPEN")
                            .build();

                    return sessionRepository.save(newSession);
                });
    }

    /**
     * Update device last seen timestamp
     * @param browserTokenHash Hashed browser token
     */
    @Transactional
    public void updateDeviceLastSeen(String browserTokenHash) {
        terminalDeviceRepository.findByTokenHashAndRevokedAtIsNull(browserTokenHash)
                .ifPresent(device -> {
                    device.updateLastSeen();
                    terminalDeviceRepository.save(device);
                });
    }
}