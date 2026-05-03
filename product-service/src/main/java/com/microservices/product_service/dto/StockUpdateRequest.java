package com.microservices.product_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUpdateRequest {

    @NotNull(message = "Stok miktarı boş olamaz")
    @Min(value = 0, message = "Stok negatif olamaz")
    private Integer stock;
}

