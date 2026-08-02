package com.ecommerce.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank(message = "sku is required") String sku,
        @NotBlank(message = "name is required") String name,
        String description,
        @NotNull(message = "categoryId is required") UUID categoryId,
        @Positive(message = "priceCents must be positive") long priceCents,
        @NotBlank(message = "currency is required") String currency,
        @PositiveOrZero(message = "initialQty must be zero or positive") int initialQty) {
}
