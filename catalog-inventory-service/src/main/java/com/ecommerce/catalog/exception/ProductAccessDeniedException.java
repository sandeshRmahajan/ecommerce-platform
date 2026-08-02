package com.ecommerce.catalog.exception;

import java.util.UUID;

public class ProductAccessDeniedException extends RuntimeException {

    public ProductAccessDeniedException(UUID productId) {
        super("You do not have permission to modify product '" + productId + "'");
    }
}
