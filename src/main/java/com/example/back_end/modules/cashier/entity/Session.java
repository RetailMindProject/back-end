package com.example.back_end.modules.cashier.entity;

import com.example.back_end.modules.terminal.entity.Terminal;
import com.example.back_end.modules.register.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "terminal_id")
    private Long terminalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", insertable = false, updatable = false)
    private Terminal terminal;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "opened_at", updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "opening_float", precision = 12, scale = 2)
    private BigDecimal openingFloat = BigDecimal.ZERO;

    @Column(name = "closing_amount", precision = 12, scale = 2)
    private BigDecimal closingAmount;

    @Column(name = "status", length = 20)
    private String status = "OPEN";

    /**
     * Session status constants
     */
    public static class SessionStatus {
        public static final String OPEN = "OPEN";
        public static final String CLOSED = "CLOSED";
    }
}