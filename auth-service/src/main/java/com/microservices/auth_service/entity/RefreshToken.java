package com.microservices.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Refresh token kaydı.
 *
 * Access token süresi dolduğunda, client bu token ile
 * POST /api/auth/refresh-token çağırarak yeni access token alır.
 *
 * Güvenlik özellikleri:
 *  - Tek kullanımlık: used = true yapıldıktan sonra tekrar kullanılamaz.
 *  - Süreli: expiresAt geçmişse reddedilir.
 *  - DB'de saklanır: logout işleminde token geçersiz kılınabilir.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Token sahibinin auth_user ID'si.
     * AuthUser'a JPA FK değil, sadece ID referansı (aynı servis içinde ama yalın tutuluyor).
     */
    @Column(nullable = false)
    private Long userId;

    /** UUID tabanlı güçlü rastgele token değeri */
    @Column(nullable = false, unique = true)
    private String token;

    /** Token son kullanma tarihi */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** Kullanılmış mı? Tek kullanımlık güvenlik için. */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    /** Ne zaman oluşturuldu */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ─────────────────────────────────────────────────────────────
    // Domain metodları
    // ─────────────────────────────────────────────────────────────

    /** Token süresi dolmuş mu? */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /** Token geçerli mi? (süresi dolmamış VE kullanılmamış) */
    public boolean isValid() {
        return !used && !isExpired();
    }

    /** Token'ı kullanıldı olarak işaretle */
    public void markAsUsed() {
        this.used = true;
    }
}

