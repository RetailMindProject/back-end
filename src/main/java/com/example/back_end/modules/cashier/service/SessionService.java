package com.example.back_end.modules.cashier.service;

import com.example.back_end.modules.cashier.dto.CashierDetailsDTO;
import com.example.back_end.modules.cashier.dto.CloseSessionRequest;
import com.example.back_end.modules.cashier.dto.SessionCardDTO;
import com.example.back_end.modules.cashier.dto.SessionFilterDTO;

import java.util.List;

public interface SessionService {

    /**
     * Get all sessions with filters for Sessions List Page
     */
    List<SessionCardDTO> getAllSessions(SessionFilterDTO filter);

    /**
     * Get cashier details for Cashier Details Page
     */
    CashierDetailsDTO getCashierDetails(Long sessionId);

    /**
     * Get all active sessions
     */
    List<SessionCardDTO> getActiveSessions();

    /**
     * Close a cashier session by ID
     * Updates the session status to CLOSED, sets closedAt = now, and optionally saves closingAmount
     */
    SessionCardDTO closeSession(Long sessionId, CloseSessionRequest request);
}