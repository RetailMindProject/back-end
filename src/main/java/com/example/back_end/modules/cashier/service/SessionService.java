package com.example.back_end.modules.cashier.service;
import com.example.back_end.modules.cashier.dto.CashierDetailsDTO;
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
}