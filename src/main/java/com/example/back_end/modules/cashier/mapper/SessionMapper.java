package com.example.back_end.modules.cashier.mapper;
import com.example.back_end.modules.cashier.dto.CashierDetailsDTO;
import com.example.back_end.modules.cashier.dto.SessionCardDTO;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.sales.order.entity.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper for Session-related entities and DTOs
 */
@Component
public class SessionMapper {

    public SessionCardDTO toSessionCardDTO(Session session, Long ordersCount, BigDecimal totalSales) {
        User user = session.getUser();

        return SessionCardDTO.builder()
                .cashierId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .sessionId(session.getId())
                .openedAt(session.getOpenedAt())
                .status(session.getStatus().name())
                .ordersCount(ordersCount)
                .totalSales(totalSales)
                .build();
    }

    public CashierDetailsDTO.CashierInfo toCashierInfo(User user) {
        return CashierDetailsDTO.CashierInfo.builder()
                .cashierId(user.getId())
                .name(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .active(user.getIsActive())
                .build();
    }

    public CashierDetailsDTO.SessionInfo toSessionInfo(Session session) {
        return CashierDetailsDTO.SessionInfo.builder()
                .sessionId(session.getId())
                .openedAt(session.getOpenedAt())
                .openingFloat(session.getOpeningFloat())
                .closingAmount(session.getClosingAmount())
                .build();
    }

    public CashierDetailsDTO.RecentTransaction toRecentTransaction(Order order) {
        return CashierDetailsDTO.RecentTransaction.builder()
                .orderNumber(order.getOrderNumber())
                .time(order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt())
                .amount(order.getGrandTotal())
                .build();
    }
}
