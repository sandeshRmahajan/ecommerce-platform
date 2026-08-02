package com.ecommerce.catalog.exception;

import java.util.UUID;

// Not currently thrown by OrderReservationListener's all-or-nothing check logic, which uses a
// boolean hasAvailableStock() check and an early return instead - kept available for potential
// use in a direct synchronous "check availability" code path elsewhere in the service.
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID productId) {
        super("Insufficient stock for product '" + productId + "'");
    }
}
