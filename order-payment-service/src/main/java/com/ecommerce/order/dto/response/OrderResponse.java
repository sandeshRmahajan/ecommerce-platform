package com.ecommerce.order.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String status,
        List<OrderItemResponse> items,
        long totalCents,
        String currency,
        // Nullable in practice - an order may not have a payment attempt yet, e.g. right after
        // creation before the saga progresses.
        PaymentResponse payment,
        Instant createdAt
) {}
