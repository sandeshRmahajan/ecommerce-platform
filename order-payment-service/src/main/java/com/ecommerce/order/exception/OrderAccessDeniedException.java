package com.ecommerce.order.exception;

import java.util.UUID;

public class OrderAccessDeniedException extends RuntimeException {

    public OrderAccessDeniedException(UUID orderId) {
        super("You do not have permission to view order '" + orderId + "'");
    }
}
