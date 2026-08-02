package com.ecommerce.order.dto.response;

// status is exposed as a plain String (not the PaymentStatus enum directly), same decoupling
// rationale used in catalog-inventory-service's ProductResponse - keeps the API contract
// independent of our internal enum structure.
// failureReason is deliberately NOT included here: internal payment gateway failure
// details/error codes should not be exposed directly to API clients, since they may contain
// gateway-internal information not meant for end users; a generic status of "FAILED" is
// sufficient for the client, with real diagnostic detail available only through internal
// logs/admin tooling.
public record PaymentResponse(
        String status,
        String gatewayRef
) {}
