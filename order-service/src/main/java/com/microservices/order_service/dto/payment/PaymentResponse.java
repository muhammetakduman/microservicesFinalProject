package com.microservices.order_service.dto.payment;

import lombok.*;

/**
 * payment-service'den dönen ödeme cevabı.
 * Doğrudan REST çağrısı yapılmadığı için şimdilik bilgi amaçlıdır.
 * İleride Feign client entegrasyonunda kullanılacak.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    /** İlgili sipariş ID'si */
    private Long orderId;

    /** Ödeme durumu: SUCCESS, FAILED, PENDING */
    private String status;

    /** Ödeme sağlayıcısından dönen işlem ID'si */
    private String transactionId;

    /** Açıklama mesajı */
    private String message;
}

