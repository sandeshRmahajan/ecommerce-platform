package com.ecommerce.order.saga;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.entity.Payment;
import com.ecommerce.order.entity.ProcessedEvent;
import com.ecommerce.order.entity.enums.OrderStatus;
import com.ecommerce.order.event.EventEnvelope;
import com.ecommerce.order.event.OrderCancelledEvent;
import com.ecommerce.order.event.OrderConfirmedEvent;
import com.ecommerce.order.event.PaymentFailedEvent;
import com.ecommerce.order.event.PaymentSucceededEvent;
import com.ecommerce.order.event.StockReservationFailedEvent;
import com.ecommerce.order.event.StockReservedEvent;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.ecommerce.order.repository.ProcessedEventRepository;
import com.ecommerce.order.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// This is where the saga's second half lives: reacting to events published by
// catalog-inventory-service (stock reservation outcomes) by progressing the order through
// payment and to its final CONFIRMED or CANCELLED state, publishing further events via the same
// outbox pattern used in OrderService so this listener's own database writes and its own
// outgoing events stay atomic together, exactly like checkout() does.
@Component
@RequiredArgsConstructor
public class OrderSagaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaListener.class);

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.stock.reserved", groupId = "order-payment-service")
    @Transactional
    public void onStockReserved(String message) throws JsonProcessingException {
        // The whole method runs inside one @Transactional boundary: the order status changes,
        // the payment record, and all outbox event writes either all commit together or all
        // roll back together, extending the same atomicity guarantee OrderService.checkout()
        // established to this side of the saga too.

        // constructParametricType is needed here because EventEnvelope<T> is a generic type, and
        // Java's type erasure means Jackson cannot infer T from a plain EventEnvelope.class
        // reference alone at runtime - this explicitly tells Jackson "deserialize the payload
        // field specifically as a StockReservedEvent".
        EventEnvelope<StockReservedEvent> envelope = objectMapper.readValue(
                message, objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, StockReservedEvent.class));

        // Idempotency short-circuit - this exact event has already been handled in a previous
        // delivery attempt.
        if (alreadyProcessed(envelope.eventId())) {
            return;
        }

        Order order = orderRepository.findById(envelope.payload().orderId()).orElse(null);
        if (order == null) {
            // Defensive - an order referenced by an incoming event should always exist, but a
            // missing order should not crash the listener/block the consumer, just be logged for
            // investigation.
            log.warn("Received stock.reserved event for unknown order {}", envelope.payload().orderId());
            return;
        }

        order.transitionTo(OrderStatus.PAYMENT_PENDING);
        Payment payment = paymentService.processPayment(order);

        if (payment.isSuccessful()) {
            order.transitionTo(OrderStatus.CONFIRMED);
            publishEvent("Order", order.getId(), "payment.succeeded", order.getId().toString(),
                    new PaymentSucceededEvent(order.getId(), payment.getId(), payment.getAmountCents()));
            publishEvent("Order", order.getId(), "order.confirmed", order.getId().toString(),
                    new OrderConfirmedEvent(order.getId(), order.getUserId()));
        } else {
            order.transitionTo(OrderStatus.CANCELLED);
            publishEvent("Order", order.getId(), "payment.failed", order.getId().toString(),
                    new PaymentFailedEvent(order.getId(), "Payment declined"));
            publishEvent("Order", order.getId(), "order.cancelled", order.getId().toString(),
                    new OrderCancelledEvent(order.getId(), "Payment failed"));
        }

        markProcessed(envelope.eventId());
    }

    @KafkaListener(topics = "inventory.stock.reservation-failed", groupId = "order-payment-service")
    @Transactional
    public void onStockReservationFailed(String message) throws JsonProcessingException {
        EventEnvelope<StockReservationFailedEvent> envelope = objectMapper.readValue(
                message, objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, StockReservationFailedEvent.class));

        if (alreadyProcessed(envelope.eventId())) {
            return;
        }

        Order order = orderRepository.findById(envelope.payload().orderId()).orElse(null);
        if (order == null) {
            log.warn("Received stock.reservation-failed event for unknown order {}", envelope.payload().orderId());
            return;
        }

        order.transitionTo(OrderStatus.CANCELLED);
        publishEvent("Order", order.getId(), "order.cancelled", order.getId().toString(),
                new OrderCancelledEvent(order.getId(), envelope.payload().reason()));

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

    // Centralizes the outbox-write logic so both listener methods (and OrderService, if
    // refactored later) share one consistent way of publishing events through the outbox -
    // avoiding copy-pasted envelope/serialization code in every place an event needs to be
    // published.
    private void publishEvent(String aggregateType, UUID aggregateId, String eventType, String traceId, Object payload)
            throws JsonProcessingException {
        EventEnvelope<Object> envelope = EventEnvelope.of(eventType, traceId, payload);
        String jsonPayload = objectMapper.writeValueAsString(envelope);
        outboxEventRepository.save(new OutboxEvent(aggregateType, aggregateId, eventType, jsonPayload));
    }
}
