package com.ecommerce.order.event;

import java.time.Instant;
import java.util.UUID;

// Generic wrapper (note the <T> type parameter) so the SAME envelope shape can carry any
// specific event payload type (OrderCreatedEvent, PaymentSucceededEvent, etc.) without
// duplicating eventId/traceId/version handling in every individual event class.
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String traceId,
        int version,
        T payload
) {
    // Callers never have to manually generate a UUID or call Instant.now() themselves - this is
    // the single place those two side-effecting calls happen, keeping event-creation code
    // elsewhere simple and consistent.
    public static <T> EventEnvelope<T> of(String eventType, String traceId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), eventType, Instant.now(), traceId, 1, payload);
    }
}
