package com.microservices.shopping_cart_service.event;

import com.microservices.shopping_cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * cart-service RabbitMQ Event Dinleyicisi.
 *
 * cart.clear.queue dinlenir:
 *   - PaymentSuccessEvent geldiğinde order-service, payment-service veya
 *     stok-service bu kuyruğa mesaj gönderir.
 *   - Müşterinin sepeti otomatik olarak temizlenir.
 *
 * Beklenen payload: { "customerId": 1, "orderId": 5 }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CartEventListener {

    private final CartService cartService;

    /**
     * Sipariş onaylandıktan sonra sepeti temizle.
     *
     * Sipariş akışı:
     *  OrderConfirmed → payment-service → PaymentSuccess
     *  → cart.clear.queue → CartEventListener → CartService.clearCart()
     */
    @RabbitListener(queues = "${rabbitmq.queue.cart-clear:cart.clear.queue}")
    public void handleCartClear(Map<String, Object> payload) {
        // Payload'dan customerId'yi çıkar (Jackson Long veya Integer döndürebilir)
        Object raw = payload.get("customerId");
        if (raw == null) {
            log.error("cart.clear event'inde customerId bulunamadı – payload: {}", payload);
            return;
        }

        Long customerId = raw instanceof Long l ? l : ((Integer) raw).longValue();
        Long orderId    = payload.get("orderId") instanceof Long l ? l
                         : payload.get("orderId") instanceof Integer i ? i.longValue() : null;

        log.info("CartClear event alındı – customerId: {}, orderId: {}", customerId, orderId);

        // Müşterinin sepetini temizle
        cartService.clearCart(customerId);

        log.info("Sepet temizlendi – customerId: {}", customerId);
    }
}

