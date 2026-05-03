package com.microservices.order_service.repository;

import com.microservices.order_service.entity.Order;
import com.microservices.order_service.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Order CRUD repository.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Belirli bir müşterinin tüm siparişleri */
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /** Belirli bir müşterinin belirli durumdaki siparişleri */
    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);

    /** Tüm siparişler — admin paneli için */
    List<Order> findAllByOrderByCreatedAtDesc();
}

