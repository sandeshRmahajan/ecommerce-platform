package com.ecommerce.order.event;

import java.util.UUID;

// Published by this service once an order reaches CONFIRMED status, for other services (like a
// future notification-service) to react to.
public record OrderConfirmedEvent(
        UUID orderId,
        UUID userId
) {}
