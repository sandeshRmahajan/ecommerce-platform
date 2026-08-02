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
@Table(name = "order_items", schema = "order_payment")
@Getter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Same cross-service-boundary reasoning as Product.supplierId in catalog-inventory-service:
    // the product lives in catalog-inventory-service's own database, so it can only ever be a raw
    // UUID reference here, not a JPA relationship.
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    // Denormalized SNAPSHOT of the product's name at the time of order, deliberately duplicated
    // from catalog-inventory-service rather than looked up live, because an order's historical
    // record should show what the product was called at purchase time even if the product is
    // later renamed or deleted in the catalog.
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(nullable = false)
    private int qty;

    // Same "snapshot, not live lookup" reasoning as productName, since prices change over time but
    // a past order's line items must reflect what was actually charged.
    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    public OrderItem(Order order, UUID productId, String productName, int qty, long unitPriceCents) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.qty = qty;
        this.unitPriceCents = unitPriceCents;
    }

    public long lineTotalCents() {
        return (long) qty * unitPriceCents;
    }
}
