package com.microservices.order_service.repository;

import com.microservices.order_service.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * OrderItem CRUD repository.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** Belirli siparişe ait tüm kalemler */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Belirli bir satıcının tüm sipariş kalemlerini bulur.
     * Satıcı kendi CONFIRMED siparişlerdeki ürünlerini bu sorguyla görür.
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.sellerId = :sellerId ORDER BY oi.id DESC")
    List<OrderItem> findBySellerId(@Param("sellerId") Long sellerId);
}

