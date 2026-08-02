package com.ecommerce.order.event;

import java.util.UUID;

// Published by this service.
public record PaymentSucceededEvent(
        UUID orderId,
        UUID paymentId,
        long amountCents
) {}
