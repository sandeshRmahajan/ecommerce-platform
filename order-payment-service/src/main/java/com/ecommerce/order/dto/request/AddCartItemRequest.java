package com.ecommerce.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

// TEMPORARY - unitPriceCents is accepted directly from the client for now as a stand-in for a
// real server-side lookup via a future gRPC call to catalog-inventory-service. This is a known,
// deliberate gap: a real deployment must never trust client-supplied pricing, since it would let
// anyone set their own prices. This field exists solely to allow the checkout saga to be built
// and tested end-to-end before the gRPC integration exists, and MUST be replaced with a
// server-side lookup before this service is used with real, untrusted clients.
public record AddCartItemRequest(
        @NotNull(message = "productId is required") UUID productId,
        @Positive(message = "qty must be positive") int qty,
        @Positive(message = "unitPriceCents is required") long unitPriceCents
) {}
