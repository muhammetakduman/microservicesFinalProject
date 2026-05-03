package com.microservices.payment_service.listener;

import com.microservices.payment_service.dto.PaymentTriggeredEvent;
import com.microservices.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * SAGA ödeme dinleyicisi.
 *
 * order-service, stok rezervasyonu tamamlandığında
 * payment.triggered.queue'ya PaymentTriggeredEvent gönderir.
 * Bu listener mesajı alır ve iyzico ödeme akışını başlatır.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentService paymentService;

    /**
     * order-service'den gelen ödeme tetikleme mesajı.
     * JSON → PaymentTriggeredEvent deserialize edilir.
     */
    @RabbitListener(queues = "${rabbitmq.queue.payment-triggered:payment.triggered.queue}")
    public void handlePaymentTriggered(PaymentTriggeredEvent event) {
        log.info("PaymentTriggeredEvent alındı — orderId: {}, amount: {}",
                event.getOrderId(), event.getAmount());

        paymentService.processPayment(
                event.getOrderId(),
                event.getCustomerId(),
                event.getAmount(),
                event.getCardNumber(),
                event.getCardHolderName(),
                event.getExpireMonth(),
                event.getExpireYear(),
                event.getCvc(),
                event.getCustomerEmail()
        );
    }
}

