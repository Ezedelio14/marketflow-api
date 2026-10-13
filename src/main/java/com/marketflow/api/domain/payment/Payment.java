package com.marketflow.api.domain.payment;

import com.marketflow.api.domain.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_order", columnList = "order_id"),
                @Index(name = "idx_payments_provider_payment", columnList = "provider_payment_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends AuditableEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "paid_at")
    private Instant paidAt;

    public static Payment initiate(UUID orderId, String provider, String providerPaymentId, BigDecimal amount, String currency) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.provider = provider;
        payment.providerPaymentId = providerPaymentId;
        payment.amount = amount;
        payment.currency = currency;
        payment.status = PaymentStatus.PENDING;
        return payment;
    }

    public void markSucceeded(Instant when) {
        status = PaymentStatus.SUCCEEDED;
        paidAt = when;
        failureReason = null;
    }

    public void markFailed(String reason) {
        if (status == PaymentStatus.SUCCEEDED) return;
        status = PaymentStatus.FAILED;
        failureReason = reason;
    }

    public void markCancelled() {
        if (status == PaymentStatus.PENDING || status == PaymentStatus.FAILED) {
            status = PaymentStatus.CANCELLED;
        }
    }

    public void markRefundPending() {
        status = PaymentStatus.REFUND_PENDING;
    }

    public void markRefunded() {
        status = PaymentStatus.REFUNDED;
    }

    public boolean isSucceeded() {
        return status == PaymentStatus.SUCCEEDED;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }
}
