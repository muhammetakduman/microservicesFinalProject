package com.microservices.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Kimlik doğrulama bilgilerini tutan entity.
 *
 * ÖNEMLİ SORUMLULUK SINIRI:
 * ─────────────────────────────────────────────────────────────────
 * Bu entity SADECE kimlik bilgisi taşır (email, password, role).
 * Ad, soyad, adres, telefon → user-service'dedir (ayrı DB).
 *
 * Kayıt akışı:
 *  1. Bu entity auth_db'ye kaydedilir.
 *  2. auth-service, ürettiği id ile user-service'e Feign ile profil oluşturma isteği gönderir.
 *  3. user-service User.id = this.id olarak profili kaydeder.
 * ─────────────────────────────────────────────────────────────────
 *
 * UserDetails: Spring Security'nin kimlik doğrulama altyapısıyla entegrasyon.
 */
@Entity
@Table(name = "auth_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kullanıcının benzersiz e-postası */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * BCrypt ile hashlenmiş şifre.
     * Düz metin şifre HİÇBİR ZAMAN saklanmaz.
     */
    @Column(nullable = false)
    private String passwordHash;

    /** Kullanıcı rolü: ADMIN, SELLER, CUSTOMER */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Hesap aktif mi? Admin devre dışı bırakabilir. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Kayıt zamanı */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ─────────────────────────────────────────────────────────────
    // Spring Security — UserDetails implementasyonu
    // ─────────────────────────────────────────────────────────────

    /**
     * Kullanıcının yetkilerini döner.
     * Rol "ROLE_" prefiksiyle döndürülür (Spring Security standardı).
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** Spring Security'nin şifre alanı — hashlenmiş değer döner */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /** Spring Security'nin kullanıcı adı alanı — email kullanılır */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active; // Devre dışı hesap kilitli sayılır
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}

