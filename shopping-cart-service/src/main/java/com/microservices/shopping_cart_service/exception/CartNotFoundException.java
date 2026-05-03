package com.microservices.shopping_cart_service.exception;

/**
 * Müşteriye ait sepet bulunamadığında fırlatılır.
 */
public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(Long customerId) {
        super("Sepet bulunamadı — customerId: " + customerId);
    }
}

