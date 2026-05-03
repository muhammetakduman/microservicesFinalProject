package com.microservices.order_service.event;

import lombok.*;

import java.util.List;

/**
 * order-service'in RabbitMQ'ya publish ettiği event.
 *
 * Routing key : order.created
 * Hedef queue  : order.created.queue
 * Dinleyenler  : stok-service (stok rezervasyonu için)
 *
 * JSON yapısı stok-service'deki EventPayloads.OrderCreatedEvent ile
 * birebir aynıdır — Jackson serialize/deserialize edebilir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {

    private Long orderId;
    private Long customerId;

    /** Her bir ürün ve istenen miktar */
    private List<OrderItem> items;

    /**
     * Stok-service'in rezerve edeceği tek bir ürün kalemi.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Long productId;
        private Integer quantity;
    }
}

