package com.microservices.payment_service.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * POST /api/payments/init — REST yoluyla manuel ödeme başlatma.
 * SAGA akışı dışında frontend'den direkt tetikleme için.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitRequest {
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String cardNumber;
    private String cardHolderName;
    private String expireMonth;
    private String expireYear;
    private String cvc;
    /** Mail bildirimi için müşteri e-posta adresi */
    private String customerEmail;
}

