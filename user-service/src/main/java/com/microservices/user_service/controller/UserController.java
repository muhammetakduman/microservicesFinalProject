package com.microservices.user_service.controller;

import com.microservices.user_service.dto.CreateUserRequest;
import com.microservices.user_service.dto.UpdateUserRequest;
import com.microservices.user_service.dto.UserResponse;
import com.microservices.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Kullanıcı Profil REST API.
 *
 * Endpoint'ler (docs/03-api-contracts.md):
 *
 * ── Profil yönetimi (müşteri) ──────────────────────────────────
 *   GET  /api/users/me?userId=1          → Kendi profilim
 *   PUT  /api/users/me?userId=1          → Profili güncelle
 *   GET  /api/users/{userId}             → ID ile profil getir (Feign/admin)
 *
 * ── Internal (auth-service çağırır) ───────────────────────────
 *   POST /api/users/profile              → Profil oluştur (kayıt sırasında)
 *
 * ── Admin ──────────────────────────────────────────────────────
 *   GET  /api/users/admin                → Tüm kullanıcılar
 *   PUT  /api/users/admin/{userId}/deactivate → Hesabı dondur
 *   PUT  /api/users/admin/{userId}/activate   → Hesabı aktif et
 *
 * NOT: userId şu an @RequestParam — auth-service tamamlandığında
 *      JWT içinden çekilecek (X-User-Id header veya SecurityContext).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    // ──────────────────────────────────────────────────────────────
    // INTERNAL — auth-service tarafından çağrılır
    // ──────────────────────────────────────────────────────────────

    /**
     * POST /api/users/profile
     * auth-service kayıt akışında kullanıcı profili oluşturmak için çağırır.
     * Body: { "userId": 1, "email": "a@b.com", "firstName": "Ali", "lastName": "Kaya" }
     */
    @PostMapping("/profile")
    public ResponseEntity<UserResponse> createProfile(
            @Valid @RequestBody CreateUserRequest request) {
        log.info("POST /api/users/profile – email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createProfile(request));
    }

    // ──────────────────────────────────────────────────────────────
    // MÜŞTERİ endpoint'leri
    // ──────────────────────────────────────────────────────────────

    /**
     * GET /api/users/me?userId=1
     * Oturum açan kullanıcının kendi profilini döner.
     * Auth service tamamlanınca JWT'den çekilecek.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@RequestParam Long userId) {
        log.info("GET /api/users/me – userId: {}", userId);
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    /**
     * PUT /api/users/me?userId=1
     * Kullanıcı kendi profilini günceller.
     * Body: { "firstName": "Yeni", "phone": "05551234567", "address": {...} }
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @RequestParam Long userId,
            @RequestBody UpdateUserRequest request) {
        log.info("PUT /api/users/me – userId: {}", userId);
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    // ──────────────────────────────────────────────────────────────
    // ORTAK — Feign client & admin tarafından kullanılır
    // ──────────────────────────────────────────────────────────────

    /**
     * GET /api/users/{userId}
     * Diğer servisler (order-service, seller-service) OpenFeign ile bu endpoint'i çağırır.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        log.info("GET /api/users/{}", userId);
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    /**
     * GET /api/users/by-email?email=a@b.com
     * auth-service token yenileme veya doğrulama sırasında kullanır.
     */
    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {
        log.info("GET /api/users/by-email – email: {}", email);
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    // ──────────────────────────────────────────────────────────────
    // ADMİN endpoint'leri
    // ──────────────────────────────────────────────────────────────

    /**
     * GET /api/users/admin
     * Tüm kullanıcı profillerini listeler.
     */
    @GetMapping("/admin")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /api/users/admin");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * PUT /api/users/admin/{userId}/deactivate
     * Kullanıcı hesabını dondurur (soft delete).
     */
    @PutMapping("/admin/{userId}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long userId) {
        log.info("PUT /api/users/admin/{}/deactivate", userId);
        return ResponseEntity.ok(userService.deactivateUser(userId));
    }

    /**
     * PUT /api/users/admin/{userId}/activate
     * Dondurulmuş hesabı yeniden aktif eder.
     */
    @PutMapping("/admin/{userId}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long userId) {
        log.info("PUT /api/users/admin/{}/activate", userId);
        return ResponseEntity.ok(userService.activateUser(userId));
    }
}

