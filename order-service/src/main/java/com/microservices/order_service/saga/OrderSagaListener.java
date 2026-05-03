package com.microservices.order_service.saga;

import com.microservices.order_service.dto.payment.PaymentRequest;
import com.microservices.order_service.entity.Order;
import com.microservices.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * SAGA Olay Dinleyicisi.
 *
 * Bu class, Saga Choreography Pattern'ın order-service ayağıdır.
 *
 * SAGA AKIŞI:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  1. Müşteri sipariş verir                                       │
 * │     OrderService → [order.created.queue] → stok-service         │
 * │                                                                  │
 * │  2a. Stok yeterliyse:                                           │
 * │     stok-service → [stock.reserved.queue] → OrderSagaListener  │
 * │     → Order durumu: STOCK_RESERVED                              │
 * │     → PaymentCardStore'dan kart bilgisi alınır                  │
 * │     → [payment.triggered.queue] → payment-service              │
 * │                                                                  │
 * │  2b. Stok yetersizse:                                           │
 * │     stok-service → [stock.not-available.queue] → OrderSagaList  │
 * │     → Order durumu: FAILED                                      │
 * │     → PaymentCardStore temizlenir                               │
 * │                                                                  │
 * │  3a. Ödeme başarılıysa:                                         │
 * │     payment-service → [payment.success.queue] → OrderSagaList   │
 * │     → Order durumu: CONFIRMED                                   │
 * │                                                                  │
 * │  3b. Ödeme başarısızsa:                                         │
 * │     payment-service → [payment.failed.queue] → OrderSagaList    │
 * │     → Order durumu: FAILED                                      │
 * │     → stok-service otomatik serbest bırakır                    │
 * └─────────────────────────────────────────────────────────────────┘
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaListener {

    private final OrderRepository orderRepository;
    private final PaymentCardStore paymentCardStore;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange:marketplace.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.payment-triggered:payment.triggered}")
    private String paymentTriggeredRoutingKey;

    // ========================
    // ADIM 2a: Stok Rezerve Edildi
    // ========================

    /**
     * stok-service stok rezervasyonunu tamamladı.
     * Sıradaki adım: ödemeyi tetikle.
     *
     * Beklenen payload: { "orderId": 1 }
     */
    @RabbitListener(queues = "${rabbitmq.queue.stock-reserved:stock.reserved.queue}")
    @Transactional
    public void handleStockReserved(Map<String, Object> payload) {
        Long orderId = extractLong(payload, "orderId");
        log.info("StockReservedEvent alındı – orderId: {}", orderId);

        // Siparişi bul ve durumunu STOCK_RESERVED yap
        Order order = findOrder(orderId);
        order.markStockReserved();
        orderRepository.save(order);

        // PaymentCardStore'dan kart bilgisini al ve payment-service'e gönder
        paymentCardStore.get(orderId).ifPresentOrElse(paymentRequest -> {
            // Tutarı siparişten güncelle (kart store'daki tutar sipariş oluşturmadan geliyor)
            paymentRequest.setAmount(order.getTotalAmount());
            rabbitTemplate.convertAndSend(exchange, paymentTriggeredRoutingKey, paymentRequest);
            log.info("Ödeme tetiklendi – orderId: {}, tutar: {}", orderId, order.getTotalAmount());
        }, () -> {
            // Kart bilgisi yoksa (sistem hatası) siparişi başarısız işaretle
            log.error("Kart bilgisi bulunamadı – orderId: {}. Sipariş FAILED yapılıyor.", orderId);
            order.fail();
            orderRepository.save(order);
        });
    }

    // ========================
    // ADIM 2b: Stok Yetersiz
    // ========================

    /**
     * stok-service stok bulamadı veya yetersiz buldu.
     * Sipariş başarısız işaretlenir, kart bilgisi temizlenir.
     *
     * Beklenen payload: { "orderId": 1, "productId": 5, "reason": "Stok yetersiz" }
     */
    @RabbitListener(queues = "${rabbitmq.queue.stock-not-available:stock.not-available.queue}")
    @Transactional
    public void handleStockNotAvailable(Map<String, Object> payload) {
        Long orderId = extractLong(payload, "orderId");
        String reason = (String) payload.getOrDefault("reason", "Stok yetersiz");
        log.warn("StockNotAvailableEvent alındı – orderId: {}, sebep: {}", orderId, reason);

        Order order = findOrder(orderId);
        order.fail();
        orderRepository.save(order);

        // Kart bilgisini temizle — ödeme artık yapılmayacak
        paymentCardStore.remove(orderId);
        log.info("Sipariş FAILED yapıldı – orderId: {}", orderId);
    }

    // ========================
    // ADIM 3a: Ödeme Başarılı
    // ========================

    /**
     * payment-service ödemeyi başarıyla tamamladı.
     * Sipariş CONFIRMED durumuna geçer.
     *
     * Beklenen payload: { "orderId": 1, "customerId": 2, ... }
     */
    @RabbitListener(queues = "${rabbitmq.queue.payment-success:payment.success.queue}")
    @Transactional
    public void handlePaymentSuccess(Map<String, Object> payload) {
        Long orderId = extractLong(payload, "orderId");
        log.info("PaymentSuccessEvent alındı – orderId: {}", orderId);

        Order order = findOrder(orderId);
        order.confirm();
        orderRepository.save(order);

        // Kart bilgisini temizle — artık gerekmiyor
        paymentCardStore.remove(orderId);
        log.info("Sipariş CONFIRMED yapıldı – orderId: {}", orderId);

        // TODO: notification-service'e "Siparişiniz onaylandı" eventi gönder
        // TODO: cart-service'e "Sepeti temizle" eventi gönder
    }

    // ========================
    // ADIM 3b: Ödeme Başarısız
    // ========================

    /**
     * payment-service ödemeyi reddetti.
     * Sipariş FAILED durumuna geçer.
     * stok-service zaten payment.failed.queue'yu dinliyor ve stoku serbest bırakıyor.
     *
     * Beklenen payload: { "orderId": 1, "reason": "Kart reddedildi" }
     */
    @RabbitListener(queues = "${rabbitmq.queue.payment-failed:payment.failed.queue}")
    @Transactional
    public void handlePaymentFailed(Map<String, Object> payload) {
        Long orderId = extractLong(payload, "orderId");
        String reason = (String) payload.getOrDefault("reason", "Ödeme reddedildi");
        log.warn("PaymentFailedEvent alındı – orderId: {}, sebep: {}", orderId, reason);

        Order order = findOrder(orderId);
        order.fail();
        orderRepository.save(order);

        // Kart bilgisini temizle
        paymentCardStore.remove(orderId);
        log.info("Sipariş FAILED yapıldı – orderId: {}", orderId);

        // TODO: notification-service'e "Ödemeniz başarısız" eventi gönder
    }

    // ========================
    // Yardımcı metodlar
    // ========================

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı – orderId: " + orderId));
    }

    /** Map'ten Long değer çeker (Integer veya Long olabilir — Jackson'dan gelen JSON tipine göre) */
    private Long extractLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        throw new IllegalArgumentException("Payload'da '" + key + "' alanı bulunamadı");
    }
}

