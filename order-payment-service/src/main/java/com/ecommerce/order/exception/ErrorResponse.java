package com.ecommerce.order.exception;

import java.time.Instant;

public record ErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        Instant timestamp
) {
    public static ErrorResponse of(String type, String title, int status, String detail, String instance) {
        return new ErrorResponse(type, title, status, detail, instance, Instant.now());
    }
}
