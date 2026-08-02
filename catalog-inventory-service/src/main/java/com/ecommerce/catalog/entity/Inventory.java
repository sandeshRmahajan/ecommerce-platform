package com.ecommerce.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "inventory", schema = "catalog")
public class Inventory {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    @Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Inventory(UUID productId, int initialQty) {
        this.productId = productId;
        this.availableQty = initialQty;
        this.reservedQty = 0;
        this.reorderThreshold = 10;
    }

    // This is a simple read-only check for callers that need to verify stock before acting.
    public boolean hasAvailableStock(int qty) {
        return availableQty >= qty;
    }

    // This method intentionally does not validate availability itself; the calling service should
    // check hasAvailableStock() first so reserve() stays a focused state mutation and remains easy
    // to reuse and test in isolation. This is also the method whose update benefits from the
    // @Version optimistic lock, so concurrent reservations on the same row will fail with
    // OptimisticLockException instead of silently overwriting each other.
    public void reserve(int qty) {
        this.availableQty -= qty;
        this.reservedQty += qty;
    }

    // This returns stock to the available pool when a reservation is cancelled or expires.
    public void release(int qty) {
        this.availableQty += qty;
        this.reservedQty -= qty;
    }
}
