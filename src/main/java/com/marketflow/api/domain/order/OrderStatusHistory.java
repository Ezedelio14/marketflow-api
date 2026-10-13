package com.marketflow.api.domain.order;

import com.marketflow.api.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Audit trail of every state transition performed by the {@code OrderStateMachine}.
 */
@Getter
@Entity
@Table(name = "order_status_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private OrderStatus toStatus;

    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    static OrderStatusHistory record(Order order, OrderStatus from, OrderStatus to, String reason, String changedBy) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.order = order;
        history.fromStatus = from;
        history.toStatus = to;
        history.reason = reason;
        history.changedBy = changedBy;
        history.changedAt = Instant.now();
        return history;
    }
}
