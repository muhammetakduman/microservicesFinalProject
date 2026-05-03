package com.microservices.product_service.repository;

import com.microservices.product_service.entity.Product;
import com.microservices.product_service.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Sayfalı - 100M ürünü tek seferde çekmek yerine sayfa sayfa
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndStatus(String name, ProductStatus status, Pageable pageable);

    // RabbitMQ listener için hızlı tekil sorgu
    List<Product> findBySellerId(Long sellerId);
}

