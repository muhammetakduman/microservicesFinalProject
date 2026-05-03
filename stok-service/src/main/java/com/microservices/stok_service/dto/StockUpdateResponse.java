package com.microservices.stok_service.dto;

import lombok.*;

/**
 * Stok güncelleme veya sorgulama sonucu dönen DTO.
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

