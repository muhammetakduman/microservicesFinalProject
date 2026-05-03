package com.microservices.payment_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

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
    /**
     * Sipariş kalemleri — stok commit/release için SAGA eventlerine aktarılır.
     * Stok takibi yapılmıyorsa null veya boş gönderilebilir.
     */
    private List<PaymentTriggeredEvent.OrderItem> items;
}
