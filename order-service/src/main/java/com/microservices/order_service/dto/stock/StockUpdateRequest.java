package com.microservices.order_service.dto.stock;

import lombok.*;

/**
 * stok-service'e gönderilen REST stok güncelleme isteği.
 * İleride OpenFeign ile stok-service çağrılacak.
 * Operasyon: INCREASE veya DECREASE
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUpdateRequest {

    private Long productId;
    private Integer quantity;

    /** "INCREASE" veya "DECREASE" */
    private String operation;
}

