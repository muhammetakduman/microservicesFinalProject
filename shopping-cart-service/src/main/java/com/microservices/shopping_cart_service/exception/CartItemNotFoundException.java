package com.microservices.shopping_cart_service.exception;

/**
 * Sepette aranılan ürün kalemi bulunamadığında fırlatılır.
 */
public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long itemId) {
        super("Sepet kalemi bulunamadı — itemId: " + itemId);
    }

    public CartItemNotFoundException(Long cartId, Long productId) {
        super("Sepette ürün bulunamadı — cartId: " + cartId + ", productId: " + productId);
    }
}

