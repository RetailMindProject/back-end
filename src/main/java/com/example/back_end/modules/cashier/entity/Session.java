package com.example.back_end.modules.sales.session.entity;
import com.example.back_end.modules.cashier.entity.Terminal;
import com.example.back_end.modules.register.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id")
    private Terminal terminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
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

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.OPEN;

    public enum SessionStatus {
        OPEN,
        CLOSED
    }

    public Long getUserId() {
        return userId;
    }
}