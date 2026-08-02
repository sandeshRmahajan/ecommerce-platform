package com.ecommerce.order.entity.enums;

/**
 * INITIATED is set when a payment attempt is first created (before the gateway responds),
 * SUCCEEDED/FAILED reflect the gateway's response, and REFUNDED is a separate later transition
 * only reachable from SUCCEEDED, used if an order is cancelled or returned after payment has
 * already completed.
 */
public enum PaymentStatus {
    INITIATED,
    SUCCEEDED,
    FAILED,
    REFUNDED
}
