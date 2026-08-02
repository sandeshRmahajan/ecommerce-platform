package com.ecommerce.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank(message = "name is required") String name,
        UUID parentId) {
}
