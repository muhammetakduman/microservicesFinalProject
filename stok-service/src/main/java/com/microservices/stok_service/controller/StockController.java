package com.microservices.stok_service.controller;

import com.microservices.stok_service.dto.StockUpdateRequest;
import com.microservices.stok_service.dto.StockUpdateResponse;
import com.microservices.stok_service.service.StockDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Stok REST API.
 * Seller / Admin / diğer servisler (Feign) buradan erişir.
 */
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
@Slf4j
public class StockController {

    private final StockDomainService stockDomainService;

    /**
     * GET /api/v1/stocks/{productId}
     * Belirli bir ürünün stok bilgisini döner.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<StockUpdateResponse> getStock(@PathVariable Long productId) {
        log.info("Stok sorgusu – productId: {}", productId);
        return ResponseEntity.ok(stockDomainService.getStock(productId));
    }

    /**
     * POST /api/v1/stocks
     * Yeni stok kaydı oluşturur (ürün onaylanınca product-service çağırır).
     */
    @PostMapping
    public ResponseEntity<StockUpdateResponse> createStock(
            @RequestParam Long productId,
            @RequestParam String productName,
            @RequestParam Long sellerId,
            @RequestParam Integer quantity) {
        log.info("Yeni stok oluşturma isteği – productId: {}", productId);
        return ResponseEntity.ok(
                stockDomainService.createStock(productId, productName, sellerId, quantity));
    }

    /**
     * PATCH /api/v1/stocks
     * Stok miktarını günceller (INCREASE / DECREASE).
     * Body: { "productId": 1, "quantity": 10, "operation": "INCREASE" }
     */
    @PatchMapping
    public ResponseEntity<StockUpdateResponse> updateStock(@RequestBody StockUpdateRequest request) {
        log.info("Stok güncelleme isteği – productId: {}, operasyon: {}",
                request.getProductId(), request.getOperation());
        return ResponseEntity.ok(stockDomainService.updateStock(request));
    }
}

