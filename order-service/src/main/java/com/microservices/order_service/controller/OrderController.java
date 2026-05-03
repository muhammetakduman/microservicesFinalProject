package com.microservices.order_service.controller;

import com.microservices.order_service.dto.CreateOrderRequest;
import com.microservices.order_service.dto.OrderResponse;
import com.microservices.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order REST API.
 *
 * Endpointler docs/03-api-contracts.md'den alınmıştır.
 *
 * Müşteri endpoint'leri:
 *   POST /api/orders                         → Sipariş oluştur (SAGA başlatır)
 *   GET  /api/orders/my-orders?customerId=1  → Siparişlerim
 *   GET  /api/orders/{orderId}               → Sipariş detayı
 *   PUT  /api/orders/{orderId}/cancel        → İptal et
 *
 * Satıcı endpoint'leri:
 *   GET /api/orders/seller/items?sellerId=1                         → Satış kalemlerim
 *   PUT /api/orders/seller/items/{orderItemId}/status?sellerId=1    → Kalem durumu güncelle
 *
 * Admin endpoint'leri:
 *   GET /api/orders/admin                    → Tüm siparişler
 *   GET /api/orders/admin/{orderId}          → Sipariş detayı
 *   PUT /api/orders/admin/{orderId}/status   → Durum güncelle
 *
 * NOT: customerId ve sellerId şu an request param olarak alınıyor.
 *      Auth service entegre edilince JWT'den çekilecek.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    // ========================
    // MÜŞTERİ ENDPOINTLERİ
    // ========================

    /**
     * POST /api/orders
     * Yeni sipariş oluşturur.
     *
     * customerId ve customerEmail — JWT'den gelir (Gateway X-User-* header'ları).
     * Request body'de gönderilse bile JWT değerleri önceliklidir.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Email", required = false) String xUserEmail) {

        // JWT'den gelen değerler her zaman öncelikli (body'deki değerleri override et)
        if (xUserId != null && !xUserId.isBlank()) {
            request.setCustomerId(Long.parseLong(xUserId));
        }
        if (xUserEmail != null && !xUserEmail.isBlank()) {
            request.setCustomerEmail(xUserEmail);
        }

        log.info("POST /api/orders – customerId: {}, email: {}", request.getCustomerId(), request.getCustomerEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    /**
     * GET /api/orders/my-orders
     * Müşterinin kendi siparişlerini listeler — customerId JWT'den gelir.
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestParam(required = false) Long customerId) {
        Long resolvedId = (xUserId != null && !xUserId.isBlank()) ? Long.parseLong(xUserId) : customerId;
        log.info("GET /api/orders/my-orders – customerId: {}", resolvedId);
        return ResponseEntity.ok(orderService.getMyOrders(resolvedId));
    }

    /**
     * GET /api/orders/{orderId}
     * Sipariş detayını döner.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        log.info("GET /api/orders/{}", orderId);
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    /**
     * PUT /api/orders/{orderId}/cancel
     * Müşteri siparişini iptal eder — customerId JWT'den gelir.
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestParam(required = false) Long customerId) {
        Long resolvedId = (xUserId != null && !xUserId.isBlank()) ? Long.parseLong(xUserId) : customerId;
        log.info("PUT /api/orders/{}/cancel – customerId: {}", orderId, resolvedId);
        return ResponseEntity.ok(orderService.cancelOrder(orderId, resolvedId));
    }

    // ========================
    // SATICI ENDPOINTLERİ
    // ========================

    /**
     * GET /api/orders/seller/items
     * Satıcının ürünlerini içeren tüm sipariş kalemlerini döner — sellerId JWT'den gelir.
     */
    @GetMapping("/seller/items")
    public ResponseEntity<List<OrderResponse.OrderItemResponse>> getSellerItems(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestParam(required = false) Long sellerId) {
        Long resolvedId = (xUserId != null && !xUserId.isBlank()) ? Long.parseLong(xUserId) : sellerId;
        log.info("GET /api/orders/seller/items – sellerId: {}", resolvedId);
        return ResponseEntity.ok(orderService.getSellerItems(resolvedId));
    }

    /**
     * PUT /api/orders/seller/items/{orderItemId}/status
     * Satıcı kalem durumunu günceller — sellerId JWT'den gelir.
     */
    @PutMapping("/seller/items/{orderItemId}/status")
    public ResponseEntity<OrderResponse.OrderItemResponse> updateItemStatus(
            @PathVariable Long orderItemId,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestParam(required = false) Long sellerId,
            @RequestParam String status) {
        Long resolvedId = (xUserId != null && !xUserId.isBlank()) ? Long.parseLong(xUserId) : sellerId;
        log.info("PUT /api/orders/seller/items/{}/status – sellerId: {}, yeniDurum: {}",
                orderItemId, resolvedId, status);
        return ResponseEntity.ok(orderService.updateItemStatus(orderItemId, status, resolvedId));
    }

    // ========================
    // ADMİN ENDPOINTLERİ
    // ========================

    /**
     * GET /api/orders/admin
     * Tüm siparişleri listeler.
     */
    @GetMapping("/admin")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("GET /api/orders/admin");
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * GET /api/orders/admin/{orderId}
     * Admin belirli siparişin detayını görür.
     */
    @GetMapping("/admin/{orderId}")
    public ResponseEntity<OrderResponse> getOrderByIdAdmin(@PathVariable Long orderId) {
        log.info("GET /api/orders/admin/{}", orderId);
        return ResponseEntity.ok(orderService.getOrderByIdForAdmin(orderId));
    }

    /**
     * PUT /api/orders/admin/{orderId}/status?status=SHIPPED
     * Admin sipariş durumunu zorla günceller.
     */
    @PutMapping("/admin/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        log.info("PUT /api/orders/admin/{}/status – yeniDurum: {}", orderId, status);
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }
}

