package com.microservices.auth_service.security;

import com.microservices.auth_service.entity.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Servisi — HMAC-SHA256 (HS256) algoritması kullanır.
 *
 * Token yapısı:
 * ─────────────────────────────────────────────────────────────────
 * Header  : { "alg": "HS256", "typ": "JWT" }
 * Payload : {
 *   "sub"  : "1"           ← userId (String)
 *   "email": "a@b.com"     ← kullanıcı e-postası
 *   "role" : "CUSTOMER"    ← kullanıcı rolü
 *   "iat"  : 1700000000    ← oluşturulma (Unix timestamp)
 *   "exp"  : 1700086400    ← son kullanma (Unix timestamp)
 * }
 * Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
 * ─────────────────────────────────────────────────────────────────
 *
 * API Gateway bu servisle aynı secret'ı kullanarak token'ı doğrular.
 * Downstream servisler (user-service, order-service) token payload'ını
 * Gateway'in eklediği X-User-Id, X-User-Email, X-User-Role header'larından okur.
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long expirationMs; // Varsayılan 24 saat

    // ─────────────────────────────────────────────────────────────
    // Token Üretme
    // ─────────────────────────────────────────────────────────────

    /**
     * Kullanıcı için JWT access token üretir.
     *
     * @param user Kimlik bilgileri doğrulanmış kullanıcı
     * @return Signed JWT string
     */
    public String generateToken(AuthUser user) {
        Map<String, Object> claims = new HashMap<>();
        // Diğer servislerin ihtiyaç duyduğu bilgiler claim olarak eklenir
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());

        return buildToken(claims, String.valueOf(user.getId()), expirationMs);
    }

    /**
     * Token'ı imzalamak için HMAC-SHA256 anahtarı üretir.
     * Secret en az 256 bit (32 byte) olmalıdır.
     *
     * @return SecretKey
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * JWT token oluşturur ve imzalar.
     *
     * @param extraClaims Ek payload alanları (email, role)
     * @param subject     Token'ın sahibi — userId
     * @param expiration  Geçerlilik süresi (ms)
     * @return Signed JWT
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)                                      // sub = userId
                .issuedAt(new Date())                                  // iat = şimdi
                .expiration(new Date(System.currentTimeMillis() + expiration)) // exp
                .signWith(getSigningKey())                             // HS256 imzası
                .compact();
    }

    // ─────────────────────────────────────────────────────────────
    // Token Okuma / Doğrulama
    // ─────────────────────────────────────────────────────────────

    /**
     * Token'dan tüm claim'leri çıkarır.
     * İmza doğrulanır — geçersiz token'da exception fırlatır.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Token'dan userId (sub) çıkarır.
     *
     * @param token JWT string
     * @return userId (Long)
     */
    public Long extractUserId(String token) {
        return Long.parseLong(extractAllClaims(token).getSubject());
    }

    /**
     * Token'dan email çıkarır.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    /**
     * Token'dan rol çıkarır.
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Token süresi dolmuş mu?
     */
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Token verilen kullanıcı için geçerli mi?
     * İmzayı, son kullanma tarihini ve kullanıcı ID'sini kontrol eder.
     *
     * @param token JWT string
     * @param user  Doğrulama yapılacak kullanıcı
     * @return true: geçerli
     */
    public boolean isTokenValid(String token, AuthUser user) {
        try {
            Long tokenUserId = extractUserId(token);
            return tokenUserId.equals(user.getId()) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("JWT doğrulama başarısız: {}", e.getMessage());
            return false;
        }
    }
}

