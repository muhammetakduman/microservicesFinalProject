package com.microservices.product_service.mapper;

import com.microservices.product_service.dto.ProductResponse;
import com.microservices.product_service.entity.Product;
import org.springframework.stereotype.Component;

@Component

public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .sellerId(product.getSellerId())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .status(product.getStatus())
                .build();
    }
}

