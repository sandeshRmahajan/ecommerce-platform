package com.ecommerce.catalog.saga;

import com.ecommerce.catalog.entity.Inventory;
import com.ecommerce.catalog.entity.OutboxEvent;
import com.ecommerce.catalog.entity.ProcessedEvent;
import com.ecommerce.catalog.entity.Product;
import com.ecommerce.catalog.entity.StockReservation;
import com.ecommerce.catalog.event.EventEnvelope;
import com.ecommerce.catalog.event.OrderCreatedEvent;
import com.ecommerce.catalog.event.StockReservationFailedEvent;
import com.ecommerce.catalog.event.StockReservedEvent;
import com.ecommerce.catalog.repository.InventoryRepository;
import com.ecommerce.catalog.repository.OutboxEventRepository;
import com.ecommerce.catalog.repository.ProcessedEventRepository;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.catalog.repository.StockReservationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

// This is catalog-inventory-service's half of the checkout saga - reacts to order.created events
// by attempting to reserve stock for every line item, all-or-nothing (if any item lacks
// sufficient stock, no reservations are made for that order at all), then publishes the outcome
// so order-payment-service can proceed to payment or cancel the order accordingly.
@Component
@RequiredArgsConstructor
public class OrderReservationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderReservationListener.class);

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ProductRepository productRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "catalog-inventory-service")
    @Transactional
    public void onOrderCreated(String message) throws JsonProcessingException {
        // The whole method runs inside one @Transactional boundary: every Inventory update, every
        // StockReservation insert, and all outbox event writes either all commit together or all
        // roll back together, the same atomicity reasoning as OrderSagaListener.

        // constructParametricType is needed here because EventEnvelope<T> is a generic type, and
        // Java's type erasure means Jackson cannot infer T from a plain EventEnvelope.class
        // reference alone at runtime - this explicitly tells Jackson "deserialize the payload
        // field specifically as an OrderCreatedEvent".
        EventEnvelope<OrderCreatedEvent> envelope = objectMapper.readValue(
                message, objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, OrderCreatedEvent.class));

        // Idempotency short-circuit - this exact event has already been handled in a previous
        // delivery attempt.
        if (alreadyProcessed(envelope.eventId())) {
            return;
        }

        // All-or-nothing check strategy: FIRST loop through every item and verify sufficient stock
        // exists for ALL of them (without reserving anything yet), and only if every single item
        // passes that check, THEN loop again and actually perform the reservations - this two-pass
        // approach avoids partially reserving some items before discovering a later item in the
        // same order is out of stock, which would require unwinding/releasing the already-reserved
        // items.
        for (OrderCreatedEvent.OrderLineItem item : envelope.payload().items()) {
            Inventory inventory = inventoryRepository.findById(item.productId()).orElse(null);
            if (inventory == null || !inventory.hasAvailableStock(item.qty())) {
                publishEvent("Order", envelope.payload().orderId(), "inventory.stock.reservation-failed",
                        envelope.payload().orderId().toString(),
                        new StockReservationFailedEvent(envelope.payload().orderId(), "Insufficient stock for one or more items"));
                markProcessed(envelope.eventId());
                return;
            }
        }

        StockReservation lastReservation = null;
        for (OrderCreatedEvent.OrderLineItem item : envelope.payload().items()) {
            // Both lookups are guaranteed to succeed here in practice, since the first pass already
            // confirmed a matching Inventory row exists (Inventory's own @Id is the product id) for every
            // item being processed in this loop - but we use orElseThrow with IllegalStateException rather
            // than silently risking a NullPointerException, since a missing Product or Inventory row at
            // this point would indicate a genuine data-consistency problem (e.g. a Product deleted without
            // its Inventory row being cleaned up) worth failing loudly and diagnosably on, rather than
            // crashing with no context.
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Product " + item.productId() + " not found during stock reservation, despite passing the earlier availability check"));
            Inventory inventory = inventoryRepository.findById(item.productId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Inventory for product " + item.productId() + " not found during stock reservation, despite passing the earlier availability check"));
            inventory.reserve(item.qty());
            inventoryRepository.save(inventory);
            // The 15-minute expiry window matches our platform design doc's reservation-expiry
            // policy, though the actual scheduled job that auto-releases expired unconfirmed
            // reservations is not yet built (StockReservationRepository.findByStatusAndExpiresAtBefore
            // exists for this future purpose).
            lastReservation = stockReservationRepository.save(
                    new StockReservation(product, envelope.payload().orderId(), item.qty(), Instant.now().plus(15, ChronoUnit.MINUTES)));
        }

        // Known simplification: StockReservedEvent currently carries only one reservationId, which
        // doesn't fully represent a multi-item order's multiple StockReservation rows; a more
        // complete design would carry a list of reservation ids, but is left as-is for now to match
        // the event contract already established and proven working with order-payment-service,
        // which does not currently use the reservationId field's value for anything functionally
        // important. The id of the last StockReservation created is used here as a representative
        // value.
        publishEvent("Order", envelope.payload().orderId(), "inventory.stock.reserved",
                envelope.payload().orderId().toString(),
                new StockReservedEvent(envelope.payload().orderId(), lastReservation.getId()));

        markProcessed(envelope.eventId());
    }

    // The idempotency check every listener method must perform first, before doing any other
    // work, protecting against Kafka redelivering the same message.
    private boolean alreadyProcessed(UUID eventId) {
        return processedEventRepository.existsById(eventId);
    }

    private void markProcessed(UUID eventId) {
        processedEventRepository.save(new ProcessedEvent(eventId));
    }

    // Centralizes the outbox-write logic so this listener has one consistent way of publishing
    // events through the outbox - avoiding copy-pasted envelope/serialization code in every place
    // an event needs to be published.
    private void publishEvent(String aggregateType, UUID aggregateId, String eventType, String traceId, Object payload)
            throws JsonProcessingException {
        EventEnvelope<Object> envelope = EventEnvelope.of(eventType, traceId, payload);
        String jsonPayload = objectMapper.writeValueAsString(envelope);
        outboxEventRepository.save(new OutboxEvent(aggregateType, aggregateId, eventType, jsonPayload));
    }
}
