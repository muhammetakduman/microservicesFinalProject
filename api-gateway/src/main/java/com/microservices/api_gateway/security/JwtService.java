package com.microservices.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Gateway tarafında JWT doğrulama servisi.
 * Token imzalamaz — sadece doğrular ve claim okur.
 * auth-service ile AYNI secret kullanılır (HS256).
 */
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    // -------------------------
    // HMAC-SHA256 signing key
    // -------------------------
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * JWT'yi doğrular ve tüm claim'leri döner.
     * Geçersiz / süresi dolmuş token için JwtException fırlatır.
     */
    public Claims validateAndExtractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Token geçerli mi? (imza + süre kontrolü)
     */
    public boolean isValid(String token) {
        try {
            validateAndExtractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // -------------------------
    // Claim yardımcıları
    // -------------------------

    public String extractUserId(String token) {
        return validateAndExtractClaims(token).getSubject(); // sub = userId
    }

    public String extractEmail(String token) {
        return validateAndExtractClaims(token).get("email", String.class);
    }

    public String extractRole(String token) {
        return validateAndExtractClaims(token).get("role", String.class);
    }
}

