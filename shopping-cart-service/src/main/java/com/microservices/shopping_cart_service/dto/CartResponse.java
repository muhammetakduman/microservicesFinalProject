package com.microservices.shopping_cart_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /api/cart — Sepetin tüm içeriğini dönen cevap DTO'su.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private Long cartId;
    private Long customerId;

    /** Sepet kalemleri */
    private List<CartItemResponse> items;

    /** Tüm kalemlerin toplam tutarı */
    private BigDecimal totalAmount;

    /** Sepetteki toplam ürün adedi */
    private int totalItemCount;

    private LocalDateTime updatedAt;

    // -------------------------------------------------------

    /**
     * Tek bir sepet kaleminin özeti.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CartItemResponse {
        private Long itemId;
        private Long productId;
        private String productName;
        private Long sellerId;
        private BigDecimal unitPrice;
        private Integer quantity;
        /** Kalem toplam fiyatı = unitPrice × quantity */
        private BigDecimal lineTotal;
    }
}

