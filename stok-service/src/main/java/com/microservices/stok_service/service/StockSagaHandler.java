package com.microservices.stok_service.service;

import com.microservices.stok_service.dto.EventPayloads;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ SAGA koordinatörü.
 * Gelen eventleri dinler ve StockDomainService'e iletir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StockSagaHandler {

    private final StockDomainService stockDomainService;

    /** order-service → stok rezerve et */
    @RabbitListener(queues = "${rabbitmq.queue.order-created:order.created.queue}")
    public void handleOrderCreated(EventPayloads.OrderCreatedEvent event) {
        log.info("OrderCreatedEvent alındı – orderId: {}", event.getOrderId());
        stockDomainService.reserveStock(event);
    }

    /** payment-service → stok commit et (gerçekten düşür) */
    @RabbitListener(queues = "${rabbitmq.queue.payment-success:payment.success.queue}")
    public void handlePaymentSuccess(EventPayloads.PaymentSuccessEvent event) {
        log.info("PaymentSuccessEvent alındı – orderId: {}", event.getOrderId());
        stockDomainService.commitStock(event);
    }

    /** payment-service → rezervasyonu serbest bırak */
    @RabbitListener(queues = "${rabbitmq.queue.payment-failed:payment.failed.queue}")
    public void handlePaymentFailed(EventPayloads.PaymentFailedEvent event) {
        log.info("PaymentFailedEvent alındı – orderId: {}, sebep: {}", event.getOrderId(), event.getReason());
        stockDomainService.releaseStock(event);
    }
}

