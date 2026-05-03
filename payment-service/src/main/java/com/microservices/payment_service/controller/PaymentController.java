package com.microservices.payment_service.controller;

import com.microservices.payment_service.dto.PaymentInitRequest;
import com.microservices.payment_service.dto.PaymentResponse;
import com.microservices.payment_service.dto.RefundRequest;
import com.microservices.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payment REST API.
 *
 * POST /api/payments/init               → Manuel ödeme başlat (SAGA dışı)
 * POST /api/payments/callback/iyzico    → iyzico 3DS callback (webhook)
 * GET  /api/payments/order/{orderId}    → Siparişin ödeme bilgisi
 * GET  /api/payments/{paymentId}        → Ödeme detayı
 * POST /api/payments/{paymentId}/refund → İade
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // ──────────────────────────────────────────────
    // ÖDEME BAŞLAT (REST / Manuel yol)
    // SAGA akışında kullanılmaz — frontend direkt tetiklemek isterse burası
    // ──────────────────────────────────────────────

    /**
     * POST /api/payments/init
     * Frontend'in kart bilgisiyle direkt ödeme başlatmasını sağlar.
     * Normalde SAGA flow'u kullanılır (RabbitMQ), bu endpoint yedek yoldur.
     *
     * Body:
     * {
     *   "orderId": 1, "customerId": 2, "amount": 500.00,
     *   "cardNumber": "5528790000000008", "cardHolderName": "Test User",
     *   "expireMonth": "12", "expireYear": "2030", "cvc": "123",
     *   "customerEmail": "musteri@gmail.com"
     * }
     */
    @PostMapping("/init")
    public ResponseEntity<PaymentResponse> initPayment(@RequestBody PaymentInitRequest request) {
        log.info("POST /api/payments/init — orderId: {}", request.getOrderId());
        PaymentResponse response = paymentService.processPayment(
                request.getOrderId(),
                request.getCustomerId(),
                request.getAmount(),
                request.getCardNumber(),
                request.getCardHolderName(),
                request.getExpireMonth(),
                request.getExpireYear(),
                request.getCvc(),
                request.getCustomerEmail(),
                request.getItems()
        );
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────
    // iyzico 3DS CALLBACK (Webhook)
    // iyzico işlem sonucunu bu adrese POST eder (3D Secure akışında)
    // ──────────────────────────────────────────────

    /**
     * POST /api/payments/callback/iyzico
     * iyzico'nun 3DS tamamlandıktan sonra yönlendirdiği endpoint.
     * Şu an için sadece loglama yapar — 3DS entegrasyonu genişletilebilir.
     */
    @PostMapping("/callback/iyzico")
    public ResponseEntity<String> iyzicoCallback(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String conversationId) {
        log.info("iyzico callback alındı — conversationId: {}, token: {}", conversationId, token);
        // 3DS akışı genişletilecekse burada ThreedsPayment.create() çağrılır
        return ResponseEntity.ok("OK");
    }

    // ──────────────────────────────────────────────
    // SORGULAR
    // ──────────────────────────────────────────────

    /**
     * GET /api/payments/order/{orderId}
     * Siparişe ait ödeme bilgisini döner.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getByOrderId(@PathVariable Long orderId) {
        log.info("GET /api/payments/order/{}", orderId);
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }

    /**
     * GET /api/payments/{paymentId}
     * Ödeme ID'sine göre bilgi döner.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long paymentId) {
        log.info("GET /api/payments/{}", paymentId);
        return ResponseEntity.ok(paymentService.getById(paymentId));
    }

    // ──────────────────────────────────────────────
    // İADE
    // ──────────────────────────────────────────────

    /**
     * POST /api/payments/{paymentId}/refund
     * Ödemeyi iade eder. Kısmi iade için body'de amount belirtilebilir.
     *
     * Body: { "amount": 150.00, "reason": "Ürün iade edildi" }
     * Body boş gönderilirse tam iade yapılır.
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refund(
            @PathVariable Long paymentId,
            @RequestBody(required = false) RefundRequest request) {
        log.info("POST /api/payments/{}/refund", paymentId);
        return ResponseEntity.ok(paymentService.refund(
                paymentId,
                request != null ? request.getAmount() : null
        ));
    }
}

