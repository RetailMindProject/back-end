package com.example.back_end.modules.terminal.service;

import com.example.back_end.exception.DuplicateResourceException;
import com.example.back_end.exception.ResourceNotFoundException;
import com.example.back_end.modules.terminal.dto.TerminalManagementDTO;
import com.example.back_end.modules.terminal.entity.Terminal;
import com.example.back_end.modules.terminal.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for Terminal management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TerminalServiceImpl implements TerminalService {

    private final TerminalRepository terminalRepository;

    @Override
    public TerminalManagementDTO.TerminalResponse createTerminal(TerminalManagementDTO.CreateRequest request) {
        log.info("Creating new terminal with code: {}", request.getCode());

        // Validate code is unique
        if (terminalRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Terminal code already exists: " + request.getCode());
        }

        // Create terminal
        Terminal terminal = new Terminal();
        terminal.setCode(request.getCode());
        terminal.setDescription(request.getDescription());
        terminal.setIsActive(true);

        Terminal saved = terminalRepository.save(terminal);

        log.info("Terminal created successfully with ID: {}", saved.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TerminalManagementDTO.TerminalResponse> getAllTerminals() {
        log.info("Fetching all terminals");
        List<Terminal> terminals = terminalRepository.findAll();
        return terminals.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TerminalManagementDTO.TerminalResponse getTerminalById(Long id) {
        log.info("Fetching terminal with ID: {}", id);
        Terminal terminal = terminalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found with id: " + id));
        return toResponse(terminal);
    }

    @Override
    public TerminalManagementDTO.TerminalResponse updateTerminal(Long id, TerminalManagementDTO.UpdateRequest request) {
        log.info("Updating terminal with ID: {}", id);

        Terminal terminal = terminalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found with id: " + id));

        // Check if code is being changed and if new code already exists
        if (request.getCode() != null && !request.getCode().equals(terminal.getCode())) {
            if (terminalRepository.existsByCode(request.getCode())) {
                throw new DuplicateResourceException("Terminal code already exists: " + request.getCode());
            }
            terminal.setCode(request.getCode());
        }

        if (request.getDescription() != null) {
            terminal.setDescription(request.getDescription());
        }

        if (request.getIsActive() != null) {
            terminal.setIsActive(request.getIsActive());
        }

        Terminal saved = terminalRepository.save(terminal);

        log.info("Terminal updated successfully with ID: {}", saved.getId());

        return toResponse(saved);
    }

    @Override
    public void deleteTerminal(Long id) {
        log.info("Deleting terminal with ID: {}", id);

        Terminal terminal = terminalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found with id: " + id));

        // Soft delete - set isActive to false
        terminal.setIsActive(false);
        terminalRepository.save(terminal);

        log.info("Terminal deleted (deactivated) successfully with ID: {}", id);
    }

    @Override
    public TerminalManagementDTO.TerminalResponse activateTerminal(Long id) {
        log.info("Activating terminal with ID: {}", id);

        Terminal terminal = terminalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal not found with id: " + id));

        // Activate terminal - set isActive to true
        terminal.setIsActive(true);
        Terminal saved = terminalRepository.save(terminal);

        log.info("Terminal activated successfully with ID: {}", saved.getId());

        return toResponse(saved);
    }

    /**
     * Map Terminal entity to TerminalResponse DTO
     */
    private TerminalManagementDTO.TerminalResponse toResponse(Terminal terminal) {
        return TerminalManagementDTO.TerminalResponse.builder()
                .id(terminal.getId())
                .code(terminal.getCode())
                .description(terminal.getDescription())
                .isActive(terminal.getIsActive())
                .createdAt(terminal.getCreatedAt())
                .lastSeenAt(terminal.getLastSeenAt())
                .build();
    }
}

