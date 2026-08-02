package com.ecommerce.order.controller;

import com.ecommerce.order.dto.response.OrderResponse;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public OrderResponse getOrder(@AuthenticationPrincipal UUID userId, @PathVariable("id") UUID id) {
        return orderService.getOrder(userId, id);
    }

    @GetMapping
    public Page<OrderResponse> listOrders(@AuthenticationPrincipal UUID userId,
                                           @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return orderService.listOrders(userId, pageable);
    }
}
