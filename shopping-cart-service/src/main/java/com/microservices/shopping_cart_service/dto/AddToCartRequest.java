package com.microservices.shopping_cart_service.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * POST /api/cart/items — Sepete ürün ekleme isteği.
 *
 * Neden productName ve unitPrice request'te geliyor?
 * product-service ile senkron Feign çağrısı yapmak yerine
 * client ürün bilgisini iletir. Bu bilgiler snapshot olarak kartItemde saklanır.
 * İleride OpenFeign entegre edildiğinde bu alanlar otomatik doldurulacak.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {

    /** Eklenecek ürünün ID'si (product-service referansı) */
    private Long productId;

    /** Ürün adı (snapshot — sepete eklenme anındaki değer) */
    private String productName;

    /** Satıcı ID'si (seller-service referansı) */
    private Long sellerId;

    /** Birim fiyat (snapshot — sepete eklenme anındaki değer) */
    private BigDecimal unitPrice;

    /** Eklenecek adet (en az 1 olmalı) */
    private Integer quantity;
}

