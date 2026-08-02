package com.ecommerce.catalog.entity;

import com.ecommerce.catalog.entity.enums.ApprovalStatus;
import com.ecommerce.catalog.entity.enums.ProductStatus;
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
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "products", schema = "catalog")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Not every product query needs its category eagerly loaded; a simple existence check or
    // stock update can skip this entirely, so callers that do need the category should use an
    // explicit join fetch query instead of paying the cost on every load.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // This is deliberately a raw UUID rather than a @ManyToOne to a User entity because the
    // supplier lives in auth-service's own database schema and microservice boundary; a JPA
    // relationship cannot span separate databases/services, so supplier lookups must happen via
    // gRPC or REST rather than a database join.
    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Product(String sku, String name, String description, Category category, UUID supplierId,
            long priceCents, String currency) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.category = category;
        this.supplierId = supplierId;
        this.priceCents = priceCents;
        this.currency = currency;
        this.status = ProductStatus.ACTIVE;
        this.approvalStatus = ApprovalStatus.PENDING;
    }

    public void approve() {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.rejectionReason = reason;
    }

    // This follows the same "only overwrite provided fields" pattern as User.updateProfile()
    // in auth-service, so null/blank values leave existing state intact.
    public void updateDetails(String name, String description, Long priceCents) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (priceCents != null) {
            this.priceCents = priceCents;
        }
    }

    // This keeps ownership checks in one place rather than repeating the raw field comparison in
    // every service/controller layer.
    public boolean isOwnedBy(UUID userId) {
        return supplierId.equals(userId);
    }
}
