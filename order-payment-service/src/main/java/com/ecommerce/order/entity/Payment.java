package com.ecommerce.order.entity;

import com.ecommerce.order.entity.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "order_payment")
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Order lives in this same service (unlike the cross-service UUID-only references
    // elsewhere), so this is a real JPA relationship rather than a raw UUID column.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    // The external payment gateway's own transaction/reference ID (e.g. a Razorpay payment ID),
    // used to look up or reconcile the transaction on the gateway's side later.
    @Column(name = "gateway_ref", length = 255)
    private String gatewayRef;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Payment(Order order, long amountCents) {
        this.order = order;
        this.amountCents = amountCents;
        this.status = PaymentStatus.INITIATED;
    }

    // Called when the payment gateway confirms a successful charge.
    public void markSucceeded(String gatewayRef) {
        this.status = PaymentStatus.SUCCEEDED;
        this.gatewayRef = gatewayRef;
    }

    // Called when the payment gateway declines or errors, or when a timeout/network failure
    // prevents confirming the charge.
    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    // Convenience check used by the service layer to decide whether to confirm the associated
    // order or trigger cancellation.
    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCEEDED;
    }
}
