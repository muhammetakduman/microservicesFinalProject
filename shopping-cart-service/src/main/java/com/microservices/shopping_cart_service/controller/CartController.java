package com.microservices.shopping_cart_service.controller;

import com.microservices.shopping_cart_service.dto.AddToCartRequest;
import com.microservices.shopping_cart_service.dto.CartResponse;
import com.microservices.shopping_cart_service.dto.UpdateCartItemRequest;
import com.microservices.shopping_cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Alışveriş Sepeti REST API.
 *
 * Endpoint'ler docs/03-api-contracts.md'den:
 *
 *   GET    /api/cart?customerId=1              → Sepeti görüntüle
 *   POST   /api/cart/items?customerId=1        → Ürün ekle
 *   PUT    /api/cart/items/{itemId}            → Kalem miktarı güncelle
 *   DELETE /api/cart/items/{itemId}            → Kalem sil
 *   DELETE /api/cart/clear?customerId=1        → Sepeti temizle
 *
 * NOT: customerId şu an request param olarak alınıyor.
 *      Auth service entegre edilince JWT'den çekilecek.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    /**
     * GET /api/cart?customerId=1
     * Müşterinin mevcut sepetini döner.
     * Sepet yoksa otomatik boş sepet oluşturulur.
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestParam Long customerId) {
        log.info("GET /api/cart – customerId: {}", customerId);
        return ResponseEntity.ok(cartService.getCart(customerId));
    }

    /**
     * POST /api/cart/items?customerId=1
     * Sepete ürün ekler. Aynı ürün varsa miktarı artırılır.
     *
     * Body örneği:
     * {
     *   "productId": 5,
     *   "productName": "Laptop",
     *   "sellerId": 2,
     *   "unitPrice": 15000.00,
     *   "quantity": 1
     * }
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @RequestParam Long customerId,
            @RequestBody AddToCartRequest request) {
        log.info("POST /api/cart/items – customerId: {}, productId: {}", customerId, request.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(customerId, request));
    }

    /**
     * PUT /api/cart/items/{itemId}?customerId=1
     * Sepetteki bir kalemin miktarını günceller.
     *
     * Body: { "quantity": 3 }
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
            @RequestParam Long customerId,
            @PathVariable Long itemId,
            @RequestBody UpdateCartItemRequest request) {
        log.info("PUT /api/cart/items/{} – customerId: {}, yeniMiktar: {}", itemId, customerId, request.getQuantity());
        return ResponseEntity.ok(cartService.updateItem(customerId, itemId, request));
    }

    /**
     * DELETE /api/cart/items/{itemId}?customerId=1
     * Sepetten belirli bir kalemi kaldırır.
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @RequestParam Long customerId,
            @PathVariable Long itemId) {
        log.info("DELETE /api/cart/items/{} – customerId: {}", itemId, customerId);
        return ResponseEntity.ok(cartService.removeItem(customerId, itemId));
    }

    /**
     * DELETE /api/cart/clear?customerId=1
     * Müşterinin tüm sepetini temizler (Manuel temizlik).
     * Otomatik temizlik cart.clear.queue üzerinden CartEventListener tarafından yapılır.
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(@RequestParam Long customerId) {
        log.info("DELETE /api/cart/clear – customerId: {}", customerId);
        cartService.clearCart(customerId);
        return ResponseEntity.noContent().build();
    }
}

