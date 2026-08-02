package com.ecommerce.catalog.event;

import java.util.List;
import java.util.UUID;

// Published by order-payment-service, consumed here to trigger stock reservation. This event
// type does not exist as a producer in THIS service - catalog-inventory-service only consumes
// it. The definition lives here purely so this service's @KafkaListener has a Java type to
// deserialize the incoming JSON into.
public record OrderCreatedEvent(
        UUID orderId,
        List<OrderLineItem> items
) {

    // items intentionally only carries productId and qty, matching the exact same shape
    // order-payment-service actually publishes - no price or product name fields to keep this
    // deserialization type in sync with the real producer contract.
    // Nested (rather than a separate top-level file) and public so any code in a different
    // package within this service can reference OrderCreatedEvent.OrderLineItem directly.
    public record OrderLineItem(UUID productId, int qty) {}
}
