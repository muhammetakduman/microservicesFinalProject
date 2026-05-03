package com.microservices.product_service.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Ürün bulunamadı. ID: " + id);
    }
}

