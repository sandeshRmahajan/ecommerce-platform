package com.ecommerce.catalog.event;

import java.util.UUID;

// Published by this service (catalog-inventory-service) once stock has been successfully
// reserved for an order, consumed by order-payment-service.
public record StockReservedEvent(
        UUID orderId,
        UUID reservationId
) {}
