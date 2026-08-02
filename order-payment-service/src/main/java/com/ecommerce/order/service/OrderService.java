package com.ecommerce.order.service;

import com.ecommerce.order.dto.request.CheckoutRequest;
import com.ecommerce.order.dto.response.OrderItemResponse;
import com.ecommerce.order.dto.response.OrderResponse;
import com.ecommerce.order.dto.response.PaymentResponse;
import com.ecommerce.order.entity.Cart;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.entity.Payment;
import com.ecommerce.order.entity.ShippingAddress;
import com.ecommerce.order.event.EventEnvelope;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.event.OrderCreatedEvent.OrderLineItem;
import com.ecommerce.order.exception.EmptyCartException;
import com.ecommerce.order.exception.OrderAccessDeniedException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.repository.CartRepository;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.ecommerce.order.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    // productNames would, in a fully wired system, come from a gRPC call to
    // catalog-inventory-service to fetch current product names, made by the controller layer
    // before calling this method - same "cross-service lookups stay out of this service's core
    // logic" pattern established in CartService.addItem for pricing; passed in here as a
    // pragmatic stand-in until the gRPC integration is built.
    @Transactional
    public OrderResponse checkout(UUID userId, CheckoutRequest request, Map<UUID, String> productNames) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(EmptyCartException::new);
        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException();
        }

        ShippingAddress shippingAddress = new ShippingAddress(
                request.shippingAddress().line1(),
                request.shippingAddress().city(),
                request.shippingAddress().state(),
                request.shippingAddress().zip(),
                request.shippingAddress().country()
        );

        // Currency is hardcoded to INR for now - multi-currency support is out of scope for this iteration.
        Order order = new Order(userId, shippingAddress, "INR");
        for (var item : cart.getItems()) {
            order.addItem(item.getProductId(), productNames.getOrDefault(item.getProductId(), "Unknown Product"),
                    item.getQty(), item.getUnitPriceCents());
        }
        order = orderRepository.save(order);

        // Relies on the same managed-entity dirty-checking behavior as CartService.clearCart.
        cart.clear();

        var lineItems = order.getItems().stream()
                .map(item -> new OrderLineItem(item.getProductId(), item.getQty()))
                .toList();
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(order.getId(), lineItems);
        EventEnvelope<OrderCreatedEvent> envelope = EventEnvelope.of("order.created", order.getId().toString(), orderCreatedEvent);
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize order.created event", e);
        }
        // This save and the order save above happen in the SAME @Transactional method, which is
        // the entire point of the outbox pattern - if this method fails or the transaction rolls
        // back for any reason after this point, the order save also rolls back, so we never end
        // up with an order that exists but has no corresponding event ever recorded; a separate
        // background publisher (not yet built) will later read unpublished OutboxEvent rows and
        // actually deliver them to Kafka.
        outboxEventRepository.save(new OutboxEvent("Order", order.getId(), "order.created", jsonPayload));

        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException(orderId);
        }
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::toOrderResponse);
    }

    private OrderResponse toOrderResponse(Order order) {
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getItems().stream()
                        .map(item -> new OrderItemResponse(
                                item.getProductId(),
                                item.getProductName(),
                                item.getQty(),
                                item.getUnitPriceCents(),
                                item.lineTotalCents()))
                        .toList(),
                order.getTotalCents(),
                order.getCurrency(),
                // null here correctly reflects that a payment attempt may not exist yet for this order.
                payment == null ? null : new PaymentResponse(payment.getStatus().name(), payment.getGatewayRef()),
                order.getCreatedAt()
        );
    }
}
