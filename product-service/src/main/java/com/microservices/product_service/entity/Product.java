package com.microservices.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_product_seller_id", columnList = "sellerId"),
        @Index(name = "idx_product_status", columnList = "status"),
        @Index(name = "idx_product_seller_status", columnList = "sellerId, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    private String imageUrl;

    // Seller'a ID bazlı referans (cross-service JPA yasak)
    @Column(nullable = false)
    private Long sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    public enum ProductStatus {
        PENDING, APPROVED, REJECTED
    }
}

