package com.ecommerce.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

// TEMPORARY - productNames is accepted directly from the client for now as a stand-in for a
// real server-side lookup via a future gRPC call to catalog-inventory-service to fetch current
// product names at checkout time. This is a known, deliberate gap: a real deployment must never
// trust client-supplied product names. This field exists solely to allow the checkout saga to be
// built and tested end-to-end before the gRPC integration exists, and MUST be replaced with a
// server-side lookup before this service is used with real, untrusted clients.
public record CheckoutRequest(
        // @Valid is required here to cascade validation into the nested object's own fields.
        // Without it, Bean Validation would only check that shippingAddress is non-null, but
        // would NOT validate line1, city, etc. inside it.
        @NotNull(message = "shippingAddress is required") @Valid ShippingAddressRequest shippingAddress,
        @NotBlank(message = "paymentMethod is required") String paymentMethod,
        @NotNull(message = "productNames is required") Map<UUID, String> productNames
) {}
