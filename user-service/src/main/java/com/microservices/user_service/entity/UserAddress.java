package com.microservices.user_service.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Kullanıcı adresi.
 * User entity'sine @Embedded olarak gömülür — ayrı tablo açılmaz.
 * Birden fazla adres gerekirse ileride UserAddress @Entity'e dönüştürülür.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress {

    /** Adres satırı (cadde, sokak, bina no, daire) */
    private String addressLine;

    /** İlçe */
    private String district;

    /** Şehir */
    private String city;

    /** Posta kodu */
    private String postalCode;

    /** Ülke — varsayılan: Türkiye */
    private String country;
}

