package com.microservices.payment_service.dto;

import lombok.*;

/**
 * Ödeme başarısız → order-service'e RabbitMQ ile gönderilir.
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
}

