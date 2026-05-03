package com.microservices.stok_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_stock", indexes = {
        @Index(name = "idx_stock_seller_id", columnList = "sellerId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStock {

    // productId hem PK hem de product-service referansi (cross-service JPA yasak)
    @Id
    private Long productId;

    // Urun adi snapshot - product-service'e feign atmaya gerek kalmaz
    @Column(nullable = false)
    private String productName;

    // seller-service'e ID bazlı referans
    @Column(nullable = false)
    private Long sellerId;

    // Gercek kullanilabilir stok (reserve sirasinda duser, release'de artar)
    @Column(nullable = false)
    private Integer availableQuantity;

    // Odeme beklenen siparisler icin rezerve edilen miktar
    @Column(nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    // Optimistic locking - esz zamanli stok islemlerinde race condition onler
    @Version
    private Long version;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // -------------------------------------------------------
    // Rich Domain Model - Is mantigi entity icinde
    // -------------------------------------------------------

    /**
     * Siparis olusunca stok rezerve edilir (gercek dusus yok, sadece rezerve).
     * OrderCreatedEvent → reserve()
     */
    public void reserve(int q) {
        validatePositive(q);
        if (availableQuantity < q) {
            throw new IllegalStateException(
                    "Stok yetersiz. Mevcut: " + availableQuantity + ", Istenen: " + q);
        }
        availableQuantity -= q;
        reservedQuantity += q;
    }

    /**
     * Odeme basarisiz olursa rezerv geri birakilir.
     * PaymentFailedEvent → release()
     */
    public void release(int q) {
        validatePositive(q);
        if (reservedQuantity < q) {
            throw new IllegalStateException(
                    "Rezerve stok yetersiz. Rezerve: " + reservedQuantity + ", Istenen: " + q);
        }
        reservedQuantity -= q;
        availableQuantity += q;
    }

    /**
     * Odeme basarili olursa rezerv satisa donusur (stok gercekten gider).
     * PaymentSuccessEvent → commit()
     */
    public void commit(int q) {
        validatePositive(q);
        if (reservedQuantity < q) {
            throw new IllegalStateException(
                    "Rezerve stok yetersiz. Rezerve: " + reservedQuantity + ", Istenen: " + q);
        }
        reservedQuantity -= q;
        // availableQuantity zaten reserve sirasinda dusmustu, degismez
    }

    /**
     * Seller stok ekleyince (manuel artis).
     */
    public void increase(int q) {
        validatePositive(q);
        availableQuantity += q;
    }

    /**
     * Direkt stok dusurme (eski akis veya admin islemi icin).
     */
    public void decrease(int q) {
        validatePositive(q);
        if (availableQuantity < q) {
            throw new IllegalStateException(
                    "Stok yetersiz. Mevcut: " + availableQuantity + ", Istenen: " + q);
        }
        availableQuantity -= q;
    }

    private void validatePositive(int q) {
        if (q <= 0) {
            throw new IllegalArgumentException("Miktar 0'dan buyuk olmalidir. Verilen: " + q);
        }
    }
}
