package com.ecommerce.order.event;

import java.util.UUID;

// Published by this service.
public record PaymentFailedEvent(
        UUID orderId,
        String reason
) {}
