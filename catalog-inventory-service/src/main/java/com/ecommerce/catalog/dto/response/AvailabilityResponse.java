package com.ecommerce.catalog.dto.response;

public record AvailabilityResponse(
        boolean available,
        int availableQty) {
}
