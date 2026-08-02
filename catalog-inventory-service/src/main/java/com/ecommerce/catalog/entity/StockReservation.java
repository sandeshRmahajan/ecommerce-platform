package com.ecommerce.catalog.entity;

import com.ecommerce.catalog.entity.enums.ReservationStatus;
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
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "stock_reservations", schema = "catalog")
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Product lives in this same service/schema, so a real JPA relationship is appropriate here.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // This follows the same cross-service boundary reasoning as Product.supplierId: order data is
    // owned by order-payment-service, so it can only ever be a raw UUID reference here.
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private int qty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StockReservation(Product product, UUID orderId, int qty, Instant expiresAt) {
        this.product = product;
        this.orderId = orderId;
        this.qty = qty;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.RESERVED;
    }

    // This is a simple expiration check for callers that need to decide whether a reservation is still valid.
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    // This is called when the associated order's payment succeeds and the reservation becomes permanent.
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    // This is called when the order is cancelled, payment fails, or the reservation expires unconfirmed.
    // It only updates the reservation's own status; the inventory mutation is orchestrated by the service layer.
    public void release() {
        this.status = ReservationStatus.RELEASED;
    }
}
