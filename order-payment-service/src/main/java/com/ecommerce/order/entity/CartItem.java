package com.ecommerce.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "cart_items", schema = "order_payment")
@Getter
@NoArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // Same cross-service-boundary reasoning as Product.supplierId in catalog-inventory-service:
    // Product lives in catalog-inventory-service's own database, so it can only ever be a raw
    // UUID reference here, not a JPA relationship.
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int qty;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    public CartItem(Cart cart, UUID productId, int qty, long unitPriceCents) {
        this.cart = cart;
        this.productId = productId;
        this.qty = qty;
        this.unitPriceCents = unitPriceCents;
    }

    // Computed convenience method, not a stored column - the total is always derived from qty
    // and unitPriceCents, never persisted separately, to avoid a stored "total" column silently
    // going out of sync with the values it was computed from.
    public long lineTotalCents() {
        return qty * unitPriceCents;
    }
}
