package com.microservices.order_service.dto.payment;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * payment-service'e gönderilecek ödeme isteği.
 *
 * SAGA akışı:
 *   StockReservedEvent alındı
 *   → PaymentCardStore'dan kart bilgisi çekilir
 *   → Bu DTO RabbitMQ'ya publish edilir
 *   → payment-service dinler ve ödemeyi başlatır
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    /** İlgili siparişin ID'si */
    private Long orderId;

    /** Müşteri ID'si */
    private Long customerId;

    /** Ödenecek toplam tutar */
    private BigDecimal amount;

    // ----- Kart bilgileri (SAGA süresince PaymentCardStore'da tutulur) -----

    /** Kart numarası */
    private String cardNumber;

    /** Kart üzerindeki isim */
    private String cardHolderName;

    /** Son kullanma ayı (MM) */
    private String expireMonth;

    /** Son kullanma yılı (YYYY) */
    private String expireYear;

    /** CVV / CVC */
    private String cvc;

    /** Müşterinin e-posta adresi — ödeme bildirimi için */
    private String customerEmail;

    /**
     * Sipariş kalemleri — payment-service'in PaymentSuccessEvent / PaymentFailedEvent
     * içine kopyalaması için taşınır. stok-service commit/release işlemleri için gereklidir.
     */
    private List<OrderItem> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Long productId;
        private Integer quantity;
    }
}

