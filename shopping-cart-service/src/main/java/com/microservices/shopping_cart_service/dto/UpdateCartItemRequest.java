package com.microservices.shopping_cart_service.dto;

import lombok.*;

/**
 * PUT /api/cart/items/{itemId} — Sepet kalemi miktarını güncelleme isteği.
 * Sadece quantity değiştirilebilir; productId, fiyat değiştirilemez.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCartItemRequest {

    /**
     * Yeni miktar.
     * Minimum 1 olmalı — 0 gönderilirse kalem silinmeli (DELETE endpoint kullan).
     */
    private Integer quantity;
}

