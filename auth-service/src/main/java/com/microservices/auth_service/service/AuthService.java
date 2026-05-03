package com.microservices.auth_service.service;

import com.microservices.auth_service.client.UserServiceClient;
import com.microservices.auth_service.client.dto.CreateProfileRequest;
import com.microservices.auth_service.dto.*;
import com.microservices.auth_service.entity.AuthUser;
import com.microservices.auth_service.entity.RefreshToken;
import com.microservices.auth_service.entity.Role;
import com.microservices.auth_service.exception.AuthException;
import com.microservices.auth_service.repository.AuthUserRepository;
import com.microservices.auth_service.repository.RefreshTokenRepository;
import com.microservices.auth_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Kimlik doğrulama iş mantığı.
 *
 * Sorumluluklar:
 *  - Müşteri kaydı (CUSTOMER)
 *  - Satıcı kaydı (SELLER)
 *  - Giriş / login
 *  - Access token yenileme
 *  - Çıkış (refresh token iptal)
 *  - user-service'e profil oluşturma isteği gönderme (Feign)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserServiceClient userServiceClient;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs; // 7 gün

    // ─────────────────────────────────────────────────────────────
    // Kayıt — CUSTOMER
    // ─────────────────────────────────────────────────────────────

    /**
     * Yeni müşteri kaydı.
     * POST /api/auth/register/customer
     */
    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request) {
        return register(request, Role.CUSTOMER);
    }

    // ─────────────────────────────────────────────────────────────
    // Kayıt — SELLER
    // ─────────────────────────────────────────────────────────────

    /**
     * Yeni satıcı kaydı.
     * POST /api/auth/register/seller
     */
    @Transactional
    public AuthResponse registerSeller(RegisterRequest request) {
        return register(request, Role.SELLER);
    }

    // ─────────────────────────────────────────────────────────────
    // Kayıt — ADMIN
    // ─────────────────────────────────────────────────────────────

    /**
     * Yeni admin kaydı.
     * POST /api/auth/register/admin
     * Bu endpoint yalnızca mevcut admin tarafından çağrılmalıdır.
     */
    @Transactional
    public AuthResponse registerAdmin(RegisterRequest request) {
        return register(request, Role.ADMIN);
    }

    // ─────────────────────────────────────────────────────────────
    // Ortak kayıt mantığı
    // ─────────────────────────────────────────────────────────────

    /**
     * Kayıt akışı:
     *  1. E-posta çakışma kontrolü
     *  2. Şifreyi BCrypt ile hashle
     *  3. AuthUser'ı auth_db'ye kaydet
     *  4. user-service'e Feign ile profil oluşturma isteği gönder
     *  5. JWT access token üret
     *  6. Refresh token üret ve kaydet
     *  7. AuthResponse döndür
     */
    private AuthResponse register(RegisterRequest request, Role role) {
        // 1. E-posta kontrolü
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Bu e-posta zaten kayıtlı: " + request.getEmail());
        }

        // 2-3. Kullanıcı oluştur ve kaydet
        AuthUser user = AuthUser.builder()
                .email(request.getEmail())
                // BCrypt hashing — düz metin şifre veritabanına GİRMEZ
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .active(true)
                .build();
        authUserRepository.save(user);
        log.info("Yeni kullanıcı kaydedildi – userId: {}, email: {}, rol: {}",
                user.getId(), user.getEmail(), role);

        // 4. user-service'e profil oluştur (Feign — hata olursa fallback çalışır)
        createUserProfile(user, request);

        // 5-6. Token üret
        String accessToken  = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user.getId());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ─────────────────────────────────────────────────────────────
    // Giriş — Login
    // ─────────────────────────────────────────────────────────────

    /**
     * E-posta ve şifre ile giriş.
     * POST /api/auth/login
     *
     * Spring Security AuthenticationManager devreye girer →
     * UserDetailsService → BCrypt şifre karşılaştırma
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Spring Security ile kimlik doğrulama
        // Yanlış şifre/e-posta → BadCredentialsException (GlobalExceptionHandler yakalar)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        AuthUser user = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Kullanıcı bulunamadı."));

        // Eski refresh token'ları geçersiz kıl — tek aktif token prensibi
        refreshTokenRepository.deleteAllByUserId(user.getId());

        String accessToken  = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user.getId());

        log.info("Başarılı giriş – userId: {}, email: {}", user.getId(), user.getEmail());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ─────────────────────────────────────────────────────────────
    // Token Yenileme
    // ─────────────────────────────────────────────────────────────

    /**
     * Geçerli refresh token karşılığında yeni access token döner.
     * POST /api/auth/refresh-token
     *
     * Güvenlik: refresh token tek kullanımlıktır (rotation).
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthException("Geçersiz refresh token."));

        // Token geçerlilik kontrolü
        if (!stored.isValid()) {
            throw new AuthException("Refresh token süresi dolmuş veya zaten kullanılmış.");
        }

        // Tek kullanım — kullanılmış olarak işaretle (rotation pattern)
        stored.markAsUsed();
        refreshTokenRepository.save(stored);

        AuthUser user = authUserRepository.findById(stored.getUserId())
                .orElseThrow(() -> new AuthException("Kullanıcı bulunamadı."));

        // Yeni access token + yeni refresh token
        String newAccessToken  = jwtService.generateToken(user);
        String newRefreshToken = createRefreshToken(user.getId());

        log.info("Token yenilendi – userId: {}", user.getId());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    // ─────────────────────────────────────────────────────────────
    // Çıkış
    // ─────────────────────────────────────────────────────────────

    /**
     * Kullanıcının tüm refresh token'larını geçersiz kılar.
     * POST /api/auth/logout
     *
     * @param userId JWT'den gelen kullanıcı ID'si
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
        log.info("Çıkış yapıldı, tokenlar silindi – userId: {}", userId);
    }

    // ─────────────────────────────────────────────────────────────
    // Yardımcı metodlar (private)
    // ─────────────────────────────────────────────────────────────

    /**
     * user-service'e profil oluşturma isteği gönderir.
     * Feign hata verse UserServiceClientFallback devreye girer — kayıt iptal edilmez.
     */
    private void createUserProfile(AuthUser user, RegisterRequest request) {
        try {
            CreateProfileRequest profileRequest = CreateProfileRequest.builder()
                    .userId(user.getId())       // auth-service'deki ID → user-service'de PK olur
                    .email(user.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phone(request.getPhone())
                    .build();

            userServiceClient.createProfile(profileRequest);
            log.info("user-service profil oluşturuldu – userId: {}", user.getId());
        } catch (Exception e) {
            // Feign hatası kayıt sürecini engellemez — sadece log
            log.error("user-service profil oluşturma başarısız – userId: {}: {}", user.getId(), e.getMessage());
        }
    }

    /**
     * Yeni refresh token oluşturur ve DB'ye kaydeder.
     * UUID tabanlı — tahmin edilemez, yeterince uzun.
     *
     * @param userId Token sahibinin ID'si
     * @return Oluşturulan token değeri (String)
     */
    private String createRefreshToken(Long userId) {
        String tokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(tokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .used(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    /**
     * AuthResponse DTO'yu çeşitli metodlar için tek noktada oluşturur (DRY).
     */
    private AuthResponse buildAuthResponse(AuthUser user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // ADMIN — Kullanıcı Yönetimi
    // ─────────────────────────────────────────────────────────────

    /**
     * Tüm kayıtlı kullanıcıları listeler.
     * GET /api/auth/admin/users
     */
    public List<AuthUserResponse> getAllUsers() {
        return authUserRepository.findAll()
                .stream()
                .map(u -> AuthUserResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .active(u.isActive())
                        .createdAt(u.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * Kullanıcının rolünü günceller.
     * PUT /api/auth/admin/users/{userId}/role
     */
    @Transactional
    public AuthUserResponse updateUserRole(Long userId, Role newRole) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Kullanıcı bulunamadı: " + userId));
        user.setRole(newRole);
        authUserRepository.save(user);
        log.info("Kullanıcı rolü güncellendi — userId: {}, yeniRol: {}", userId, newRole);
        return AuthUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Kullanıcı hesabını devre dışı bırakır.
     * PUT /api/auth/admin/users/{userId}/deactivate
     */
    @Transactional
    public AuthUserResponse deactivateUser(Long userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Kullanıcı bulunamadı: " + userId));
        user.setActive(false);
        refreshTokenRepository.deleteAllByUserId(userId); // oturumu zorla sonlandır
        authUserRepository.save(user);
        log.info("Kullanıcı devre dışı bırakıldı — userId: {}", userId);
        return AuthUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .active(false)
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Devre dışı bırakılmış kullanıcıyı yeniden aktive eder.
     * PUT /api/auth/admin/users/{userId}/activate
     */
    @Transactional
    public AuthUserResponse activateUser(Long userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Kullanıcı bulunamadı: " + userId));
        user.setActive(true);
        authUserRepository.save(user);
        log.info("Kullanıcı aktive edildi — userId: {}", userId);
        return AuthUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .active(true)
                .createdAt(user.getCreatedAt())
                .build();
    }
}

