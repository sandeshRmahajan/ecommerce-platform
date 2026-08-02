package com.ecommerce.order.dto.response;

import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String productName,
        int qty,
        long unitPriceCents,
        long lineTotalCents
) {}
