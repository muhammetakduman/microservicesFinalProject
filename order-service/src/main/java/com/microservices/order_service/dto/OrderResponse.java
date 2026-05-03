package com.microservices.order_service.dto;

import com.microservices.order_service.entity.OrderDetails;
import com.microservices.order_service.entity.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sipariş endpoint'lerinden dönen birleşik cevap DTO'su.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long orderId;
    private Long customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private OrderDetails shippingDetails;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Sipariş kalemi özeti.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemResponse {
        private Long itemId;
        private Long productId;
        private String productName;
        private Long sellerId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private OrderStatus itemStatus;
    }
}

