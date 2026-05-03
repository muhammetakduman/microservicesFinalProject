package com.microservices.payment_service.dto;

import lombok.*;

import java.math.BigDecimal;

/** POST /api/payments/{paymentId}/refund isteği */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {
    /** İade edilecek tutar (kısmi iade desteklenir) */
    private BigDecimal amount;
    /** İade sebebi */
    private String reason;
}

