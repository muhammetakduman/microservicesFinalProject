package com.microservices.product_service.event;

import lombok.*;

/**
 * Admin bir urunu onayladiginda product-service bu eventi yayinlar.
 * Stok-service bu eventi dinleyerek ilgili urun icin stok kaydi olusturur.
 *
 * Routing key: product.approved
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductApprovedEvent {
    private Long productId;
    private String productName;
    private Long sellerId;
    /** Onay anindaki stok miktari - Product.stock alanından gelir */
    private Integer quantity;
}

