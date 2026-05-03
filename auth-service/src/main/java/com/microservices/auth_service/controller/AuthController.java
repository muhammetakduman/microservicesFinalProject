package com.microservices.auth_service.controller;

import com.microservices.auth_service.dto.*;
import com.microservices.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Kimlik Doğrulama REST API.
 *
 * Tüm endpoint'ler herkese açıktır (SecurityConfig'de permitAll).
 * JWT doğrulaması API Gateway katmanında yapılır.
 *
 * Endpoint'ler (docs/03-api-contracts.md):
 *
 *   POST /api/auth/register/customer  → Müşteri kaydı
 *   POST /api/auth/register/seller    → Satıcı kaydı
 *   POST /api/auth/login              → Giriş yap
 *   POST /api/auth/refresh-token      → Access token yenile
 *   POST /api/auth/logout             → Çıkış yap
 *   GET  /api/auth/me                 → Mevcut kullanıcı bilgisi
 *
 * Postman test örneği:
 *   POST http://localhost:8081/api/auth/register/customer
 *   Body: { "email":"ahmet@mail.com","password":"123456","firstName":"Ahmet","lastName":"Yılmaz" }
 *
 *   POST http://localhost:8081/api/auth/login
 *   Body: { "email":"ahmet@mail.com","password":"123456" }
 *   → Dönen accessToken'ı diğer isteklerde Authorization: Bearer {token} olarak gönder.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register/customer
     * Yeni müşteri hesabı oluşturur.
     * Başarılı kayıtta access token + refresh token döner.
     */
    @PostMapping("/register/customer")
    public ResponseEntity<AuthResponse> registerCustomer(
            @Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register/customer – email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerCustomer(request));
    }

    /**
     * POST /api/auth/register/seller
     * Yeni satıcı hesabı oluşturur.
     * Satıcının profili onaylanmak üzere seller-service'e gider (ileride).
     */
    @PostMapping("/register/seller")
    public ResponseEntity<AuthResponse> registerSeller(
            @Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register/seller – email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerSeller(request));
    }

    /**
     * POST /api/auth/login
     * E-posta ve şifre ile giriş.
     * Dönen accessToken: Authorization: Bearer {token} header'ı ile kullanılır.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login – email: {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/auth/refresh-token
     * Süresi dolmamış refresh token ile yeni access token alır.
     * Refresh token rotation: eski token geçersiz, yeni token verilir.
     *
     * Body: { "refreshToken": "uuid-string" }
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /api/auth/refresh-token");
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * POST /api/auth/logout?userId=1
     * Kullanıcının tüm refresh token'larını geçersiz kılar.
     * Access token stateless olduğu için client tarafında silinir (localStorage vb.).
     *
     * Auth service tamamlandıktan sonra userId JWT'den alınacak.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam Long userId) {
        log.info("POST /api/auth/logout – userId: {}", userId);
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/auth/me?userId=1
     * Mevcut kullanıcının temel bilgilerini döner (token'dan alınan).
     * Tam profil için user-service /api/users/me kullanılır.
     *
     * Bu endpoint şu an basit tutuldu — API Gateway JWT header injection
     * tamamlandığında otomatik userId çekilecek.
     */
    @GetMapping("/me")
    public ResponseEntity<String> me(@RequestParam Long userId) {
        log.info("GET /api/auth/me – userId: {}", userId);
        // Basit cevap — user-service entegrasyonu tamamlandıkça genişleyecek
        return ResponseEntity.ok("userId: " + userId + " — Tam profil için GET /api/users/me");
    }
}

