package com.microservices.stok_service.dto;

import lombok.*;

/**
 * HTTP veya servisler arası stok güncelleme isteği için DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUpdateRequest {

    private Long productId;

    /** Artış için pozitif, azalış için negatif değer gönderilebilir */
    private Integer quantity;

    /** "INCREASE" | "DECREASE" | "SET" */
    private String operation;
}

