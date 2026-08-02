package com.ecommerce.order.entity;

/** Embedded as JSON inside the orders table's shipping_address column via Hibernate's native JSON support, rather than being its own table - it has no independent existence or identity apart from the order it belongs to. */
public record ShippingAddress(
    String line1,
    String city,
    String state,
    String zip,
    String country
) {}
