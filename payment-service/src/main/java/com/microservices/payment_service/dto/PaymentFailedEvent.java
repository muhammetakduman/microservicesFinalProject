package com.microservices.payment_service.dto;

import lombok.*;

import java.util.List;

/**
 * Ödeme başarısız → order-service + stok-service'e RabbitMQ ile gönderilir.
 * Routing key: payment.failed
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFailedEvent {
    private Long orderId;
    private Long customerId;
    private String reason;
    /** Mail bildirimi için müşteri e-posta adresi */
    private String customerEmail;
    /**
     * Sipariş kalemleri — stok-service bu listeyi kullanarak
     * rezervasyonu serbest bırakır (release işlemi).
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

