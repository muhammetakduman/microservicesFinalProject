package com.microservices.payment_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ödeme başarılı → order-service + stok-service'e RabbitMQ ile gönderilir.
 * Routing key: payment.success
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSuccessEvent {
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String transactionId;
    /** Mail bildirimi için müşteri e-posta adresi */
    private String customerEmail;
    /**
     * Sipariş kalemleri — stok-service bu listeyi kullanarak
     * reservedQuantity'yi sıfırlar (commit işlemi).
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

