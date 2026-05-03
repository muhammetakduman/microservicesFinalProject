package com.microservices.order_service.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Kargo/fatura adresi bilgileri.
 * Order entity'sine @Embedded olarak gömülür — ayrı tablo açılmaz.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetails {

    /** Alıcı adı soyadı */
    private String recipientName;

    /** Telefon numarası */
    private String phone;

    /** Adres satırı (cadde, sokak, bina) */
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

