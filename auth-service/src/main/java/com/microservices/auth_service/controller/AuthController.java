package com.microservices.auth_service.controller;

import com.microservices.auth_service.dto.*;
import com.microservices.auth_service.entity.Role;
import com.microservices.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Kimlik Doğrulama REST API.
 *
 * Tüm endpoint'ler herkese açıktır (SecurityConfig'de permitAll).
 * JWT doğrulaması API Gateway katmanında yapılır.
 *
 * Endpoint'ler:
 *   POST /api/auth/register/customer  → Müşteri kaydı
 *   POST /api/auth/register/seller    → Satıcı kaydı
 *   POST /api/auth/login              → Giriş yap
 *   POST /api/auth/refresh-token      → Access token yenile
 *   POST /api/auth/logout             → Çıkış yap
 *   GET  /api/auth/me                 → Mevcut kullanıcı bilgisi
 *
 * Admin endpoint'ler:
 *   POST /api/auth/register/admin               → Yeni admin oluştur
 *   GET  /api/auth/admin/users                  → Tüm kullanıcılar
 *   PUT  /api/auth/admin/users/{userId}/role    → Rol güncelle
 *   PUT  /api/auth/admin/users/{userId}/deactivate → Hesabı dondur
 *   PUT  /api/auth/admin/users/{userId}/activate   → Hesabı aktive et
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // ──────────────────────────────────────────────────────────────
    // KAYIT
    // ──────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/register/customer
     * Yeni müşteri hesabı oluşturur.
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
     */
    @PostMapping("/register/seller")
    public ResponseEntity<AuthResponse> registerSeller(
            @Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register/seller – email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerSeller(request));
    }

    /**
     * POST /api/auth/register/admin
     * Yeni admin hesabı oluşturur.
     * ⚠️  Bu endpoint yalnızca mevcut admin tarafından çağrılmalıdır.
     *     (API Gateway üzerinden rol bazlı koruma ileride eklenecek)
     */
    @PostMapping("/register/admin")
    public ResponseEntity<AuthResponse> registerAdmin(
            @Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register/admin – email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerAdmin(request));
    }

    // ──────────────────────────────────────────────────────────────
    // GİRİŞ / TOKEN
    // ──────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/login
     * E-posta ve şifre ile giriş.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login – email: {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/auth/refresh-token
     * Süresi dolmamış refresh token ile yeni access token alır.
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
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam Long userId) {
        log.info("POST /api/auth/logout – userId: {}", userId);
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/auth/me?userId=1
     * Mevcut kullanıcının temel bilgileri.
     */
    @GetMapping("/me")
    public ResponseEntity<String> me(@RequestParam Long userId) {
        log.info("GET /api/auth/me – userId: {}", userId);
        return ResponseEntity.ok("userId: " + userId + " — Tam profil için GET /api/users/me");
    }

    // ──────────────────────────────────────────────────────────────
    // ADMİN — Kullanıcı Yönetimi
    // ──────────────────────────────────────────────────────────────

    /**
     * GET /api/auth/admin/users
     * Tüm kayıtlı kullanıcıları listeler (id, email, rol, aktiflik).
     * Şifre hash'i ASLA döndürülmez.
     */
    @GetMapping("/admin/users")
    public ResponseEntity<List<AuthUserResponse>> getAllUsers() {
        log.info("GET /api/auth/admin/users");
        return ResponseEntity.ok(authService.getAllUsers());
    }

    /**
     * PUT /api/auth/admin/users/{userId}/role
     * Kullanıcının rolünü değiştirir.
     * Body: { "role": "ADMIN" }  ← ADMIN | SELLER | CUSTOMER
     */
    @PutMapping("/admin/users/{userId}/role")
    public ResponseEntity<AuthUserResponse> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody RoleUpdateRequest request) {
        log.info("PUT /api/auth/admin/users/{}/role – yeniRol: {}", userId, request.getRole());
        return ResponseEntity.ok(authService.updateUserRole(userId, request.getRole()));
    }

    /**
     * PUT /api/auth/admin/users/{userId}/deactivate
     * Hesabı dondurur — kullanıcı giriş yapamaz, token'ları iptal edilir.
     */
    @PutMapping("/admin/users/{userId}/deactivate")
    public ResponseEntity<AuthUserResponse> deactivateUser(@PathVariable Long userId) {
        log.info("PUT /api/auth/admin/users/{}/deactivate", userId);
        return ResponseEntity.ok(authService.deactivateUser(userId));
    }

    /**
     * PUT /api/auth/admin/users/{userId}/activate
     * Dondurulmuş hesabı yeniden aktive eder.
     */
    @PutMapping("/admin/users/{userId}/activate")
    public ResponseEntity<AuthUserResponse> activateUser(@PathVariable Long userId) {
        log.info("PUT /api/auth/admin/users/{}/activate", userId);
        return ResponseEntity.ok(authService.activateUser(userId));
    }
}
