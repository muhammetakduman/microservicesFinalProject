package com.microservices.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Kullanıcı profil entity'si.
 *
 * ÖNEMLİ TASARIM KARARLARI:
 * ─────────────────────────────────────────────────────────────────
 * 1. Bu entity SADECE profil bilgisi tutar (ad, soyad, telefon, adres).
 *    Şifre, JWT, rol gibi kimlik doğrulama alanları auth-service'dedir.
 *
 * 2. id alanı auth-service tarafından belirlenir.
 *    Kullanıcı auth-service'e kayıt olunca auth-service bu servise
 *    POST /api/users/profile çağırır ve kendi oluşturduğu userId'yi gönderir.
 *    Böylece iki servis arasında ID tutarlılığı sağlanır.
 *
 * 3. Diğer servisler (order-service, seller-service vb.) bu servise
 *    GET /api/users/{userId} ile Feign üzerinden ulaşır.
 * ─────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * auth-service'deki kullanıcı ID'siyle eşleşen birincil anahtar.
     * @GeneratedValue kullanılmaz — auth-service sağlar.
     */
    @Id
    private Long id;

    /** Kullanıcının kayıt e-postası (auth-service'den kopyalanır — salt gösterim) */
    @Column(nullable = false, unique = true)
    private String email;

    /** Ad */
    @Column(nullable = false)
    private String firstName;

    /** Soyad */
    @Column(nullable = false)
    private String lastName;

    /** Telefon numarası */
    private String phone;

    /** Profil fotoğrafı URL'si */
    private String profilePhotoUrl;

    /**
     * Kargo/fatura adresi.
     * @Embedded → ayrı tablo açılmaz, users tablosuna sütun olarak eklenir.
     */
    @Embedded
    private UserAddress address;

    /** Hesap aktif mi? (admin tarafından devre dışı bırakılabilir) */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Hesap oluşturulma zamanı */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Son güncelleme zamanı */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ─────────────────────────────────────────────────────────────
    // Rich Domain Model — iş mantığı metodları
    // ─────────────────────────────────────────────────────────────

    /**
     * Profilin tam adını döner (firstName + lastName).
     */
    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Hesabı devre dışı bırakır (admin işlemi).
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Hesabı yeniden aktif eder (admin işlemi).
     */
    public void activate() {
        this.active = true;
    }
}

