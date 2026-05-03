package com.microservices.product_service.dto;

import com.microservices.product_service.entity.Product.ProductStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private Long sellerId;
    private Long categoryId;
    private String categoryName;
    private ProductStatus status;
}

