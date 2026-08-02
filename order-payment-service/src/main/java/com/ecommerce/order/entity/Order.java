package com.ecommerce.order.entity;

import com.ecommerce.order.entity.enums.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "order_payment")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Same cross-service-boundary reasoning as Product.supplierId in catalog-inventory-service:
    // the user account lives in auth-service's own database, so it can only ever be a raw UUID
    // reference here, not a JPA relationship.
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_cents", nullable = false)
    private long totalCents;

    @Column(nullable = false, length = 3)
    private String currency;

    // @JdbcTypeCode(SqlTypes.JSON) tells Hibernate to serialize/deserialize this field to/from
    // JSON automatically (using Jackson under the hood) rather than needing a separate table or
    // manual JSON string handling - the whole ShippingAddress record round-trips transparently as
    // the orders.shipping_address JSONB column.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", nullable = false, columnDefinition = "jsonb")
    private ShippingAddress shippingAddress;

    // mappedBy = "order" tells Hibernate that OrderItem.order is the OWNING side of this
    // relationship (it holds the actual foreign key column), so Order itself does not get its own
    // join column - it just knows how to look up its items via that existing relationship.
    // cascade = CascadeType.ALL means operations on the Order (save, delete) automatically cascade
    // to its OrderItems - saving an Order with new items in the list saves the items too, without
    // needing to save each OrderItem individually.
    // orphanRemoval = true means if an OrderItem is removed from this list in Java code (not just
    // deleted from the DB directly), Hibernate automatically deletes that row too - keeps the
    // order's item list and the actual database rows always in sync.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Order(UUID userId, ShippingAddress shippingAddress, String currency) {
        this.userId = userId;
        this.shippingAddress = shippingAddress;
        this.currency = currency;
        this.status = OrderStatus.PENDING;
        this.totalCents = 0L;
    }

    // Creates a new OrderItem owned by this order, adds it to the in-memory list, and recalculates
    // totalCents by summing all items' lineTotalCents(). Unlike Cart.totalCents() which is always
    // computed fresh on demand, Order.totalCents is a genuinely STORED field here - because once
    // an order is placed, its total needs to be a stable, queryable, historical fact (e.g. for
    // admin reporting, "list all orders over $100") without recomputing from line items on every
    // single query; addItem() is responsible for keeping the stored value in sync at the moment
    // items are actually added, before the order is ever persisted as final.
    public void addItem(UUID productId, String productName, int qty, long unitPriceCents) {
        items.add(new OrderItem(this, productId, productName, qty, unitPriceCents));
        this.totalCents = items.stream().mapToLong(OrderItem::lineTotalCents).sum();
    }

    // Intentionally a simple, unchecked transition for now (no validation that the transition is
    // legal per the state machine) - proper transition validation belongs in the service layer,
    // which will have the business context (e.g. "why" a transition is happening) to produce a
    // meaningful error if an illegal transition is attempted, rather than the entity blindly
    // rejecting a transition with no explanation.
    public void transitionTo(OrderStatus newStatus) {
        this.status = newStatus;
    }
}
