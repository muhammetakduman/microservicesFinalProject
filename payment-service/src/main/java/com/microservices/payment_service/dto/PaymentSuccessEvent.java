package com.microservices.payment_service.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Ödeme başarılı → order-service'e RabbitMQ ile gönderilir.
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
}

