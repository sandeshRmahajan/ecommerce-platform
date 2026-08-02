package com.ecommerce.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ecommerce.auth.entity.User;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final javax.crypto.SecretKey secretKey;
    private final long accessTokenTtlSeconds;

    public JwtService(@Value("${auth.jwt.secret}") String secret,
                      @Value("${auth.jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().stream().map(role -> role.getName()).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtlSeconds, ChronoUnit.SECONDS)))
                .signWith(secretKey)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
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
