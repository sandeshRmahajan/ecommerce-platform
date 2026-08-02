package com.ecommerce.order.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtValidator {
    private final javax.crypto.SecretKey secretKey;

    public JwtValidator(@Value("${auth.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<TokenClaims> validate(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles", List.class);
            return Optional.of(new TokenClaims(userId, email, roles));
        } catch (JwtException | IllegalArgumentException ex) {
            // We deliberately do not distinguish why a token is invalid to the caller.
            return Optional.empty();
        }
    }

    public record TokenClaims(UUID userId, String email, List<String> roles) {
    }
}
