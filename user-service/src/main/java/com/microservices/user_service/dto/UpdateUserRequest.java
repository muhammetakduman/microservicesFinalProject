package com.microservices.user_service.dto;

import com.microservices.user_service.entity.UserAddress;
import lombok.*;

/**
 * Kullanıcı profil güncelleme isteği.
 *
 * PUT /api/users/me
 *
 * Güncelleme kuralları:
 *  - email değiştirilemez (auth-service yönetir)
 *  - id değiştirilemez
 *  - Yalnızca belirtilen alanlar güncellenir (null gönderilirse değişmez)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    /** Yeni ad (null → mevcut değer korunur) */
    private String firstName;

    /** Yeni soyad (null → mevcut değer korunur) */
    private String lastName;

    /** Yeni telefon numarası */
    private String phone;

    /** Profil fotoğrafı URL'si */
    private String profilePhotoUrl;

    /** Kullanıcının kargo/fatura adresi */
    private UserAddress address;
}

