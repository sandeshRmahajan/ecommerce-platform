package com.ecommerce.catalog.event;

import java.util.UUID;

// Published by this service (catalog-inventory-service) when stock reservation could not be
// fulfilled for an order, consumed by order-payment-service.
public record StockReservationFailedEvent(
        UUID orderId,
        String reason
) {}
