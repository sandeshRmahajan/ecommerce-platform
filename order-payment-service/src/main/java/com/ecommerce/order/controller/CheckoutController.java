package com.ecommerce.order.controller;

import com.ecommerce.order.dto.request.CheckoutRequest;
import com.ecommerce.order.dto.response.OrderResponse;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;

    // 202 Accepted, not 201 Created: checkout only kicks off an async saga - the order is
    // created and the saga has begun, but final confirmation happens asynchronously as Kafka
    // events flow through the saga. 202 more accurately reflects the request's semantics than
    // 201, which would imply the resource is immediately in its final state.
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderResponse checkout(@AuthenticationPrincipal UUID userId, @RequestBody @Valid CheckoutRequest request) {
        return orderService.checkout(userId, request, request.productNames());
    }
}
