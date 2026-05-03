package com.microservices.order_service.dto;

import com.microservices.order_service.dto.payment.PaymentRequest;
import com.microservices.order_service.entity.OrderDetails;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * POST /api/orders endpoint'ine gelen istek gövdesi.
 *
 * Müşteri:
 *  1. Hangi ürünleri istediğini (items) belirtir
 *  2. Kargo adresini doldurur (shippingDetails)
 *  3. Kart bilgilerini girer (paymentInfo)
 *
 * Kart bilgileri ORDER TABLOSUNA KAYDEDILMEZ.
 * Yalnızca Saga tamamlanana kadar PaymentCardStore'da in-memory tutulur.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    /**
     * Geçici alan — Auth service entegre edilene kadar.
     * Gerçek uygulamada JWT'den alınacak.
     */
    private Long customerId;

    /**
     * Müşterinin e-posta adresi — ödeme bildirimi için PaymentCardStore'a aktarılır.
     * Gerçek uygulamada JWT'den alınacak.
     */
    private String customerEmail;

    /** Sepetteki ürün kalemleri */
    private List<OrderItemRequest> items;

    /** Kargo adresi */
    private OrderDetails shippingDetails;

    /** Ödeme kart bilgileri (PaymentCardStore'a geçici olarak alınır) */
    private PaymentRequest paymentInfo;

    // -------------------------------------------------------

    /**
     * Sipariş kalemi isteği.
     * productName, sellerId, unitPrice — sipariş anındaki SNAPSHOT değerleri.
     * İleride bu bilgiler product-service Feign çağrısıyla otomatik doldurulacak.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemRequest {
        private Long productId;
        private String productName;     // snapshot
        private Long sellerId;          // snapshot
        private BigDecimal unitPrice;   // snapshot
        private Integer quantity;
    }
}

