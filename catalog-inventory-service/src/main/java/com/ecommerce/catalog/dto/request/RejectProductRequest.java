package com.ecommerce.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectProductRequest(
        @NotBlank(message = "reason is required") String reason) {
}
