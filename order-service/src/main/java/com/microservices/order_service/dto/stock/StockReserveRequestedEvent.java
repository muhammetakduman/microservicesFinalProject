package com.microservices.order_service.dto.stock;

import lombok.*;

import java.util.List;

/**
 * order-service'in stok-service'e gönderdiği stok rezervasyon event'i.
 *
 * Bu DTO, order.created.queue üzerinden publish edilir.
 * stok-service bu yapıyı EventPayloads.OrderCreatedEvent olarak okur.
 * (İki servis aynı JSON yapısını paylaşır — shared library kullanılmadığından iki ayrı class)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReserveRequestedEvent {

    /** Rezervasyon talep edilen sipariş ID'si */
    private Long orderId;

    /** Siparişi veren müşteri ID'si */
    private Long customerId;

    /** Rezerve edilmesi istenen ürün kalemleri */
    private List<StockItem> items;

    /**
     * Tek bir ürün kalemi için stok rezervasyon bilgisi.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItem {
        private Long productId;
        private Integer quantity;
    }
}

