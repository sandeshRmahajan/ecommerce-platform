package com.ecommerce.catalog.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        UUID categoryId,
        String categoryName,
        UUID supplierId,
        long priceCents,
        String currency,
        String status,
        String approvalStatus,
        String rejectionReason,
        Instant createdAt) {
    // Jackson can serialize enums directly, but exposing the API contract as plain strings keeps it
    // decoupled from our internal Java enum structure and avoids leaking enum toString() quirks.
}
