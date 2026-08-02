package com.ecommerce.catalog.dto.request;

import jakarta.validation.constraints.Positive;

public record UpdateProductRequest(
        String name,
        String description,
        @Positive(message = "priceCents must be positive") Long priceCents) {
    // Bean Validation's @Positive only validates when the value is non-null, so a null
    // priceCents (meaning "don't update this field") passes through untouched.
}
