package com.microservices.product_service.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Kategori bulunamadı. ID: " + id);
    }
}

