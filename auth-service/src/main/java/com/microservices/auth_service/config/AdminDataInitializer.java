package com.microservices.auth_service.config;

import com.microservices.auth_service.client.UserServiceClient;
import com.microservices.auth_service.client.dto.CreateProfileRequest;
import com.microservices.auth_service.entity.AuthUser;
import com.microservices.auth_service.entity.Role;
import com.microservices.auth_service.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Uygulama ayağa kalktığında sistem admin kullanıcısını oluşturur.
 *
 * Admin email : akdumanmuhammet34@gmail.com
 * İlk şifre  : Admin1234! (ilk girişten sonra değiştirin)
 *
 * Yetki kapsamı:
 *   - Ürün onaylama / reddetme
 *   - Kategori yönetimi
 *   - Stok onaylama
 *   - Kullanıcı devre dışı bırakma
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminDataInitializer implements ApplicationRunner {

    private static final String ADMIN_EMAIL    = "akdumanmuhammet34@gmail.com";
    private static final String ADMIN_PASSWORD = "Admin1234!";
    private static final String ADMIN_FIRST    = "Muhammet";
    private static final String ADMIN_LAST     = "Akduman";

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder    passwordEncoder;
    private final UserServiceClient  userServiceClient;

    @Override
    public void run(ApplicationArguments args) {
        if (authUserRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Admin kullanıcı zaten mevcut — email: {}", ADMIN_EMAIL);
            return;
        }

        // 1. auth_db'ye kaydet
        AuthUser admin = AuthUser.builder()
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .active(true)
                .build();

        authUserRepository.save(admin);
        log.info("✅ Admin kullanıcı oluşturuldu — id: {}, email: {}", admin.getId(), ADMIN_EMAIL);

        // 2. user-service'e profil gönder (opsiyonel — hata olursa engelleme yapma)
        try {
            CreateProfileRequest profileRequest = CreateProfileRequest.builder()
                    .userId(admin.getId())
                    .email(ADMIN_EMAIL)
                    .firstName(ADMIN_FIRST)
                    .lastName(ADMIN_LAST)
                    .phone("05000000000")
                    .build();

            userServiceClient.createProfile(profileRequest);
            log.info("✅ Admin profili user-service'e oluşturuldu — userId: {}", admin.getId());
        } catch (Exception e) {
            log.warn("⚠️ user-service profil oluşturulamadı (servis hazır değil olabilir): {}", e.getMessage());
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔐 ADMIN GİRİŞ BİLGİLERİ");
        log.info("   Email : {}", ADMIN_EMAIL);
        log.info("   Şifre : {}", ADMIN_PASSWORD);
        log.info("   Rol   : ADMIN");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}

