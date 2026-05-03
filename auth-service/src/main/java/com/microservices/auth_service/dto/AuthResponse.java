package com.microservices.auth_service.dto;

import com.microservices.auth_service.entity.Role;
import lombok.*;

/**
 * Kayıt ve giriş işlemlerinden dönen kimlik doğrulama cevabı.
 *
 * accessToken: Kısa süreli JWT (varsayılan 24 saat).
 *   → API isteklerinde Authorization: Bearer {token} header'ı ile gönderilir.
 *
 * refreshToken: Uzun süreli token (varsayılan 7 gün).
 *   → Sadece POST /api/auth/refresh-token endpoint'ine gönderilir.
 *   → Yeni accessToken almak için kullanılır.
 *
 * JWT payload'unda ne var? (diğer servislerin ihtiyaçları):
 *   sub      → userId (String olarak)
 *   email    → kullanıcı e-postası
 *   role     → CUSTOMER / SELLER / ADMIN
 *   iat      → token oluşturma zamanı
 *   exp      → token son kullanma zamanı
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    /** JWT access token */
    private String accessToken;

    /** Refresh token (DB'de saklanır) */
    private String refreshToken;

    /** Token türü — her zaman "Bearer" */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Kullanıcı ID'si (user-service ile eşleşir) */
    private Long userId;

    /** Kullanıcı e-postası */
    private String email;

    /** Kullanıcı rolü */
    private Role role;
}

