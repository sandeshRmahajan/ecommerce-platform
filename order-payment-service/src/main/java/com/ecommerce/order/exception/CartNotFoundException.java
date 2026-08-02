package com.ecommerce.order.exception;

import java.util.UUID;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(UUID userId) {
        super("No cart found for user '" + userId + "'");
    }
}
