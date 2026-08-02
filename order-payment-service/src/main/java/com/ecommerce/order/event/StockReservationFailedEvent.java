package com.ecommerce.order.event;

import java.util.UUID;

// Published by catalog-inventory-service, consumed here. Same reasoning as StockReservedEvent:
// order-payment-service only consumes this, it never produces it.
public record StockReservationFailedEvent(
        UUID orderId,
        String reason
) {}
