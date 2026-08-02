package com.ecommerce.auth.dto.response;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        Set<String> roles,
        Instant createdAt
) {}
