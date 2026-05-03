package com.microservices.product_service.mapper;

import com.microservices.product_service.dto.CategoryResponse;
import com.microservices.product_service.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}

