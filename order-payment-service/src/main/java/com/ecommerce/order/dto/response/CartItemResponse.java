package com.ecommerce.order.dto.response;

import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        int qty,
        long unitPriceCents,
        long lineTotalCents
) {}
