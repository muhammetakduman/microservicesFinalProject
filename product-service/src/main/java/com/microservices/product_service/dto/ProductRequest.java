package com.microservices.product_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Ürün adı boş olamaz")
    private String name;

    private String description;

    @NotNull(message = "Fiyat boş olamaz")
    @DecimalMin(value = "0.0", inclusive = false, message = "Fiyat 0'dan büyük olmalı")
    private BigDecimal price;

    @NotNull(message = "Stok boş olamaz")
    @Min(value = 0, message = "Stok negatif olamaz")
    private Integer stock;

    private String imageUrl;

    @NotNull(message = "Kategori boş olamaz")
    private Long categoryId;
}

