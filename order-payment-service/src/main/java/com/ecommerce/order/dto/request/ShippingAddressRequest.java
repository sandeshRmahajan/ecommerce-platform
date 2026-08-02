package com.ecommerce.order.dto.request;

import jakarta.validation.constraints.NotBlank;

// This is the REQUEST shape a client submits at checkout; it gets mapped into the
// entity.ShippingAddress record before being stored, kept as a separate type so validation
// annotations don't leak into the persisted/JSON-serialized entity shape.
public record ShippingAddressRequest(
        @NotBlank(message = "line1 is required") String line1,
        @NotBlank(message = "city is required") String city,
        @NotBlank(message = "state is required") String state,
        @NotBlank(message = "zip is required") String zip,
        @NotBlank(message = "country is required") String country
) {}
