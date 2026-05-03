package com.microservices.payment_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * order-service'in RabbitMQ'ya publish ettiği ödeme tetikleme eventi.
 * Queue: payment.triggered.queue
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTriggeredEvent {
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    /** Müşterinin gerçek e-posta adresi — bildirim için */
    private String customerEmail;
    private String cardNumber;
    private String cardHolderName;
    private String expireMonth;
    private String expireYear;
    private String cvc;
    /**
     * Sipariş kalemleri — stok-service commit/release işlemleri için
     * PaymentSuccessEvent / PaymentFailedEvent'e aktarılır.
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

