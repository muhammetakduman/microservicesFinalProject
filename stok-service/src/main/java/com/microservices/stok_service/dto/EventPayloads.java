package com.microservices.stok_service.dto;

import lombok.*;

import java.util.List;

/**
 * Tüm event DTO'larını tek dosyada toplar.
 * Ayrı event/ paketi yerine dto/ altında birleştirilmiştir.
 */
public class EventPayloads {

    // -------------------------------------------------------
    // ORDER-SERVICE'DEN GELEN EVENTLER
    // -------------------------------------------------------

    /** Sipariş oluşunca order-service yayınlar → stok-service dinler */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderCreatedEvent {
        private Long orderId;
        private Long customerId;
        private List<OrderItem> items;

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        public static class OrderItem {
            private Long productId;
            private Integer quantity;
        }
    }

    // -------------------------------------------------------
    // PAYMENT-SERVICE'DEN GELEN EVENTLER
    // -------------------------------------------------------

    /** Ödeme başarılı → stok commit edilir (gerçekten düşer) */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PaymentSuccessEvent {
        private Long orderId;
        private Long customerId;
        private List<OrderItem> items;

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        public static class OrderItem {
            private Long productId;
            private Integer quantity;
        }
    }

    /** Ödeme başarısız → rezervasyon serbest bırakılır */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PaymentFailedEvent {
        private Long orderId;
        private Long customerId;
        private String reason;
        private List<OrderItem> items;

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        public static class OrderItem {
            private Long productId;
            private Integer quantity;
        }
    }

    // -------------------------------------------------------
    // STOK-SERVICE'İN YAYINLADIĞI EVENTLER
    // -------------------------------------------------------

    /** Stok rezervasyonu başarılı → payment-service tetiklenir */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StockReservedEvent {
        private Long orderId;
    }

    /** Stok yetersiz → order FAILED durumuna geçer */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StockNotAvailableEvent {
        private Long orderId;
        private Long productId;
        private String reason;
    }
}

