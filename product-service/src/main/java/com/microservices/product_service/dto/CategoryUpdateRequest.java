package com.microservices.product_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Admin kategori güncelleme isteği.
 * PUT /api/products/categories/{categoryId}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryUpdateRequest {

    @NotBlank(message = "Kategori adı boş olamaz")
    private String name;

    private String description;
}

