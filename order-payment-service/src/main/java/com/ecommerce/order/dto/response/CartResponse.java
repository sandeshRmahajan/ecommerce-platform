package com.ecommerce.order.dto.response;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        List<CartItemResponse> items,
        long totalCents
) {}
