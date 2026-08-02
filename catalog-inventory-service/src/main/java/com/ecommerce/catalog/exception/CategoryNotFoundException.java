package com.ecommerce.catalog.exception;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(UUID categoryId) {
        super("Category '" + categoryId + "' not found");
    }
}
