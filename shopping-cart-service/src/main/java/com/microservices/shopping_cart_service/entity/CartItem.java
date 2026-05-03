package com.microservices.shopping_cart_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Sepetteki tek bir ürün kalemi.
 *
 * Neden CartItem? Repoda "Product.java" olarak geçse de bu entity
 * aslında product-service'deki Product'ın bir SNAPSHOT'ıdır.
 * Ürün bilgisi sepete eklendiği andaki değeri korur
 * (fiyat değişse bile sepet etkilenmez — e-ticaret best practice).
 *
 * Alanların snapshot olduğu göstergesi: productName, unitPrice, sellerId
 * bu serviste kopyalanarak saklanır; product-service'e JPA ilişkisi yoktur.
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Bu kalemin bağlı olduğu alışveriş sepeti.
     * Aynı servis içi ilişki — microservices kuralını ihlal etmez.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private ShoppingCart cart;

    /**
     * product-service'deki ürünün ID'si.
     * Sadece referans — JPA ilişkisi kurulmaz.
     */
    @Column(nullable = false)
    private Long productId;

    /** Ürünün sepete eklendiği andaki adı (snapshot) */
    @Column(nullable = false)
    private String productName;

    /** Ürünü satan satıcının ID'si (seller-service referansı) */
    @Column(nullable = false)
    private Long sellerId;

    /** Sepete eklendiği andaki birim fiyat (snapshot) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** İstenen adet */
    @Column(nullable = false)
    private Integer quantity;

    // -------------------------------------------------------
    // Rich Domain Model metodları
    // -------------------------------------------------------

    /**
     * Miktarı artırır.
     * Aynı ürün tekrar sepete eklendiğinde çağrılır.
     *
     * @param additional Eklenecek miktar (pozitif olmalı)
     */
    public void increaseQuantity(int additional) {
        if (additional <= 0) {
            throw new IllegalArgumentException("Artış miktarı pozitif olmalıdır: " + additional);
        }
        this.quantity += additional;
    }

    /**
     * Miktarı belirli bir değere günceller.
     *
     * @param newQuantity Yeni miktar (en az 1 olmalı)
     */
    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Miktar en az 1 olmalıdır: " + newQuantity);
        }
        this.quantity = newQuantity;
    }

    /**
     * Kalemin toplam fiyatı = birim fiyat × miktar.
     * DB'de tutulmaz; dinamik olarak hesaplanır.
     *
     * @return Kalem toplam fiyatı
     */
    @Transient
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

