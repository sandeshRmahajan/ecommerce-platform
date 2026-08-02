package com.ecommerce.order.entity.enums;

/**
 * Valid transitions:
 * PENDING -> STOCK_RESERVED -> PAYMENT_PENDING -> CONFIRMED
 * Any state except CONFIRMED can transition to CANCELLED or FAILED depending on where the
 * failure occurred (stock reservation failure, payment failure, etc.)
 */
public enum OrderStatus {
    PENDING,
    STOCK_RESERVED,
    PAYMENT_PENDING,
    CONFIRMED,
    CANCELLED,
    FAILED
}
