package com.example.back_end.modules.cashier.service;

import com.example.back_end.exception.BusinessRuleException;
import com.example.back_end.exception.ResourceNotFoundException;
import com.example.back_end.modules.cashier.dto.CashierDetailsDTO;
import com.example.back_end.modules.cashier.dto.CloseSessionRequest;
import com.example.back_end.modules.cashier.dto.SessionCardDTO;
import com.example.back_end.modules.cashier.dto.SessionFilterDTO;
import com.example.back_end.modules.cashier.repository.CashierOrderRepository;
import com.example.back_end.modules.sales.payment.repository.PaymentRepository;
import com.example.back_end.modules.cashier.repository.SessionRepository;
import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.register.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final CashierOrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public List<SessionCardDTO> getAllSessions(SessionFilterDTO filter) {
        log.info("Fetching sessions with filter: {}", filter);

        String cashierName = filter != null ? filter.getCashierName() : null;
        String statusFilter = filter != null ? filter.getStatus() : null;
        LocalDateTime startDate = parseStartDate(filter != null ? filter.getDate() : null);
        LocalDateTime endDate = parseEndDate(filter != null ? filter.getDate() : null);

        // Get sessions from DB
        List<Session> sessions;
        if (statusFilter != null && statusFilter.equalsIgnoreCase("ACTIVE")) {
            sessions = sessionRepository.findAllActiveSessions();
        } else if (statusFilter != null && statusFilter.equalsIgnoreCase("CLOSED")) {
            sessions = sessionRepository.findAllClosedSessions();
        } else {
            sessions = sessionRepository.findAllSessionsOrdered();
        }

        // Filter in memory
        sessions = sessions.stream()
                .filter(session -> {
                    try {
                        User user = session.getUser();

                        if (user == null) {
                            log.warn("User is null for session {}", session.getId());
                            return false;
                        }

                        // Filter by cashier name
                        if (cashierName != null && !cashierName.isEmpty()) {
                            String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                            String lastName = user.getLastName() != null ? user.getLastName() : "";
                            String fullName = (firstName + " " + lastName).toLowerCase();
                            if (!fullName.contains(cashierName.toLowerCase())) {
                                return false;
                            }
                        }

                        // Filter by date range
                        if (startDate != null && session.getOpenedAt().isBefore(startDate)) {
                            return false;
                        }
                        if (endDate != null && session.getOpenedAt().isAfter(endDate)) {
                            return false;
                        }

                        return true;
                    } catch (Exception e) {
                        log.error("Error filtering session {}: {}", session.getId(), e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());

        log.info("Found {} sessions after filtering", sessions.size());

        return sessions.stream()
                .map(this::mapToSessionCardDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CashierDetailsDTO getCashierDetails(Long sessionId) {
        log.info("Fetching cashier details for session: {}", sessionId);

        Session session = sessionRepository.findByIdWithUser(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

        User user = session.getUser();
        CashierDetailsDTO.CashierInfo cashierInfo;

        if (user == null) {
            log.warn("User is null for session {}. Creating default cashier info.", sessionId);
            cashierInfo = CashierDetailsDTO.CashierInfo.builder()
                    .cashierId(null)
                    .name("Unknown Cashier")
                    .email(null)
                    .phone(null)
                    .role("CASHIER")
                    .active(false)
                    .build();
        } else {
            cashierInfo = CashierDetailsDTO.CashierInfo.builder()
                    .cashierId(user.getId())
                    .name((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                            (user.getLastName() != null ? user.getLastName() : ""))
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole() != null ? user.getRole().name() : "CASHIER")
                    .active(user.getIsActive())
                    .build();
        }

        CashierDetailsDTO.SessionInfo sessionInfo = CashierDetailsDTO.SessionInfo.builder()
                .sessionId(session.getId())
                .openedAt(session.getOpenedAt())
                .openingFloat(session.getOpeningFloat())
                .closingAmount(session.getClosingAmount())
                .build();

        Long totalOrders = orderRepository.countPaidOrdersBySessionId(sessionId);
        BigDecimal totalSales = orderRepository.calculateTotalSalesBySessionId(sessionId);
        BigDecimal cashIn = paymentRepository.calculateCashInBySessionId(sessionId);
        BigDecimal cardIn = paymentRepository.calculateCardInBySessionId(sessionId);

        CashierDetailsDTO.Performance performance = CashierDetailsDTO.Performance.builder()
                .totalOrders(totalOrders)
                .totalSales(totalSales)
                .cashIn(cashIn)
                .cardIn(cardIn)
                .build();

        List<Order> recentOrders = orderRepository.findRecentTransactionsBySessionId(sessionId);
        List<CashierDetailsDTO.RecentTransaction> recentTransactions = recentOrders.stream()
                .map(order -> CashierDetailsDTO.RecentTransaction.builder()
                        .orderNumber(order.getOrderNumber())
                        .time(order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt())
                        .amount(order.getGrandTotal())
                        .build())
                .collect(Collectors.toList());

        return CashierDetailsDTO.builder()
                .cashierInfo(cashierInfo)
                .sessionInfo(sessionInfo)
                .performance(performance)
                .recentTransactions(recentTransactions)
                .build();
    }

    @Override
    public List<SessionCardDTO> getActiveSessions() {
        log.info("Fetching all active sessions");
        List<Session> sessions = sessionRepository.findAllActiveSessions();
        return sessions.stream()
                .map(this::mapToSessionCardDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SessionCardDTO closeSession(Long sessionId, CloseSessionRequest request) {
        log.info("Closing session {} with closing amount: {}", sessionId, request.getClosingAmount());

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        if ("CLOSED".equals(session.getStatus())) {
            throw new BusinessRuleException("Session is already closed");
        }

        session.setStatus("CLOSED");
        session.setClosedAt(LocalDateTime.now());

        if (request.getClosingAmount() != null) {
            session.setClosingAmount(request.getClosingAmount());
        }

        sessionRepository.save(session);

        log.info("Session {} closed successfully", sessionId);

        return mapToSessionCardDTO(session);
    }

    // ✅ Helper methods for date parsing
    private LocalDateTime parseStartDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay();  // 00:00:00
    }

    private LocalDateTime parseEndDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atTime(LocalTime.MAX);  // 23:59:59.999999999
    }

    private SessionCardDTO mapToSessionCardDTO(Session session) {
        try {
            User user = session.getUser();
            if (user == null) {
                log.warn("User is null for session {}", session.getId());
                return null;
            }

            Long ordersCount = orderRepository.countPaidOrdersBySessionId(session.getId());
            BigDecimal totalSales = orderRepository.calculateTotalSalesBySessionId(session.getId());

            return SessionCardDTO.builder()
                    .cashierId(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .sessionId(session.getId())
                    .openedAt(session.getOpenedAt())
                    .status(session.getStatus() != null ? session.getStatus() : "UNKNOWN")
                    .ordersCount(ordersCount)
                    .totalSales(totalSales)
                    .build();
        } catch (Exception e) {
            log.error("Error mapping session {} to DTO: {}", session.getId(), e.getMessage());
            return null;
        }
    }
}