package com.ecommerce.order.event;

import java.util.List;
import java.util.UUID;

// Published by this service when an order is created, consumed by catalog-inventory-service to
// trigger stock reservation.
public record OrderCreatedEvent(
        UUID orderId,
        List<OrderLineItem> items
) {

    // items intentionally only carries productId and qty, NOT price or product name - the consuming
    // service (catalog-inventory-service) only needs to know WHAT to reserve and HOW MUCH, not
    // pricing information, which stays out of this event to avoid leaking pricing concerns into
    // inventory logic that has no need for them.
    // Nested (rather than a separate top-level file) and public so OrderService, which lives in a
    // different package, can construct instances when building an OrderCreatedEvent to publish.
    public record OrderLineItem(UUID productId, int qty) {}
}
