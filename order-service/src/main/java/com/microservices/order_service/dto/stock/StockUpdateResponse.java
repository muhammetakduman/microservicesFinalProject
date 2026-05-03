package com.microservices.order_service.dto.stock;

import lombok.*;

/**
 * stok-service'den dönen stok bilgisi cevabı.
 * İleride OpenFeign ile stok-service çağrılacak.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUpdateResponse {

    private Long productId;
    private String productName;
    private Long sellerId;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private String message;
}

