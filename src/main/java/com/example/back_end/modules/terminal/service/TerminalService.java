package com.example.back_end.modules.terminal.service;

import com.example.back_end.modules.terminal.dto.TerminalManagementDTO;

import java.util.List;

/**
 * Service for Terminal management operations
 * Used by Store Manager to manage terminals
 */
public interface TerminalService {

    /**
     * Create a new terminal
     * @param request Create terminal request
     * @return Created terminal response
     */
    TerminalManagementDTO.TerminalResponse createTerminal(TerminalManagementDTO.CreateRequest request);

    /**
     * Get all terminals
     * @return List of all terminals
     */
    List<TerminalManagementDTO.TerminalResponse> getAllTerminals();

    /**
     * Get terminal by ID
     * @param id Terminal ID
     * @return Terminal response
     */
    TerminalManagementDTO.TerminalResponse getTerminalById(Long id);

    /**
     * Update terminal
     * @param id Terminal ID
     * @param request Update request
     * @return Updated terminal response
     */
    TerminalManagementDTO.TerminalResponse updateTerminal(Long id, TerminalManagementDTO.UpdateRequest request);

    /**
     * Delete terminal (soft delete - set isActive to false)
     * @param id Terminal ID
     */
    void deleteTerminal(Long id);

    /**
     * Activate terminal (set isActive to true)
     * @param id Terminal ID
     * @return Activated terminal response
     */
    TerminalManagementDTO.TerminalResponse activateTerminal(Long id);
}

