package com.microservices.product_service.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDecreaseEvent {

    private Long productId;
    private Integer quantity;
    private Long orderId;
}

