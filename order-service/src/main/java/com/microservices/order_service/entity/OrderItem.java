package com.microservices.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Siparişteki her bir ürün kalemi.
 *
 * Önemli: productName, sellerId, unitPrice gibi alanlar
 * sipariş anındaki değerlerin SNAPSHOT'ıdır.
 * Ürün/satıcı bilgisi sonradan değişse bile sipariş kayıtları etkilenmez.
 * (Microservices database-per-service kuralı: cross-service JPA ilişkisi yasak)
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Bu kalemin bağlı olduğu sipariş.
     * Aynı servis içi JPA ilişkisi — microservices kuralını ihlal etmez.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Ürün ID (product-service'deki ürünün ID'si — sadece referans, JPA ilişkisi değil) */
    @Column(nullable = false)
    private Long productId;

    /** Ürünün sipariş anındaki adı (snapshot) */
    @Column(nullable = false)
    private String productName;

    /** Ürünü satan satıcının ID'si (seller-service — sadece referans) */
    @Column(nullable = false)
    private Long sellerId;

    /** Sipariş anındaki birim fiyat (snapshot) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** Sipariş edilen adet */
    @Column(nullable = false)
    private Integer quantity;

    /** Toplam fiyat = unitPrice × quantity */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    /**
     * Kalemin durumu — satıcı bu alanı günceller.
     * Sipariş onaylandıktan sonra: PENDING → PREPARING → SHIPPED → DELIVERED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus itemStatus = OrderStatus.PENDING;
}

