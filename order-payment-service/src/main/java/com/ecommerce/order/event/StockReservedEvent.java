package com.ecommerce.order.event;

import java.util.UUID;

// Published by catalog-inventory-service (added there separately), consumed here. This event
// type does not exist as a producer in THIS service - order-payment-service only consumes it.
// The definition lives here purely so this service's @KafkaListener has a Java type to
// deserialize the incoming JSON into.
public record StockReservedEvent(
        UUID orderId,
        UUID reservationId
) {}
