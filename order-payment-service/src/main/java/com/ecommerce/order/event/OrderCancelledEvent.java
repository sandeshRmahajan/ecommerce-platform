package com.ecommerce.order.event;

import java.util.UUID;

// Published by this service when an order is cancelled for any reason (stock reservation
// failure OR payment failure), consumed by catalog-inventory-service to release any reserved
// stock.
public record OrderCancelledEvent(
        UUID orderId,
        String reason
) {}
