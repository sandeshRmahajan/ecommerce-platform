package com.ecommerce.order.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "carts", schema = "order_payment")
@Getter
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Same cross-service-boundary reasoning as Product.supplierId in catalog-inventory-service:
    // the user account lives in auth-service's own database, so it can only ever be a raw UUID
    // reference here, not a JPA relationship.
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // mappedBy = "cart" tells Hibernate that CartItem.cart is the OWNING side of this
    // relationship (it holds the actual foreign key column), so Cart itself does not get its own
    // join column - it just knows how to look up its items via that existing relationship.
    // cascade = CascadeType.ALL means operations on the Cart (save, delete) automatically cascade
    // to its CartItems - saving a Cart with new items in the list saves the items too, without
    // needing to save each CartItem individually.
    // orphanRemoval = true means if a CartItem is removed from this list in Java code (not just
    // deleted from the DB directly), Hibernate automatically deletes that row too - keeps the
    // cart's item list and the actual database rows always in sync.
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Cart(UUID userId) {
        this.userId = userId;
    }

    // Adding a product already present in the cart increases its quantity rather than creating a
    // duplicate line item for the same product - this matches standard e-commerce cart behavior
    // and fixes a real bug found during live saga testing, where adding the same product twice
    // produced two separate order_items rows for one product instead of one row with qty=2.
    public void addItem(UUID productId, int qty, long unitPriceCents) {
        Optional<CartItem> existing = items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().increaseQty(qty);
        } else {
            items.add(new CartItem(this, productId, qty, unitPriceCents));
        }
    }

    // Computed fresh every time, never stored, for the same reasoning as CartItem.lineTotalCents().
    public long totalCents() {
        return items.stream().mapToLong(CartItem::lineTotalCents).sum();
    }

    // Relies on orphanRemoval = true to actually delete the rows on next flush.
    public void clear() {
        items.clear();
    }
}
