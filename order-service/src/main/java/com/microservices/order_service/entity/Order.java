package com.microservices.order_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ana sipariş entity'si.
 *
 * SAGA durum makinesi bu entity üzerinde yürür:
 *   PENDING → STOCK_RESERVED → CONFIRMED
 *                             ↓ (ödeme/stok başarısız)
 *                           FAILED
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Siparişi veren müşterinin ID'si.
     * user-service'deki User entity'sine ID referansı — JPA ilişkisi değil.
     */
    @Column(nullable = false)
    private Long customerId;

    /**
     * Sipariş durumu.
     * SAGA adımları bu alanı günceller.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * Sipariş toplam tutarı.
     * Tüm kalemlerin totalPrice'larının toplamı.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Kargo adresi — ayrı tablo açmadan gömülü olarak saklanır.
     */
    @Embedded
    private OrderDetails shippingDetails;

    /**
     * Sipariş kalemleri — aynı servis içi @OneToMany ilişkisi.
     * cascade = ALL → sipariş silinince kalemler de silinir.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /** Siparişin oluşturulma zamanı */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Siparişin son güncellenme zamanı */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // -------------------------------------------------------
    // Durum güncelleme yardımcı metodları (Rich Domain Model)
    // -------------------------------------------------------

    /** Stok rezerve edildi — ödeme adımına geçiliyor */
    public void markStockReserved() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Stok rezervasyonu sadece PENDING siparişte yapılabilir. Mevcut: " + this.status);
        }
        this.status = OrderStatus.STOCK_RESERVED;
    }

    /** Ödeme başarılı — sipariş onaylandı */
    public void confirm() {
        if (this.status != OrderStatus.STOCK_RESERVED) {
            throw new IllegalStateException("Onay sadece STOCK_RESERVED siparişte yapılabilir. Mevcut: " + this.status);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    /** Sipariş başarısız (stok yetersiz veya ödeme başarısız) */
    public void fail() {
        this.status = OrderStatus.FAILED;
    }

    /** Müşteri siparişi iptal etti */
    public void cancel() {
        if (this.status == OrderStatus.CONFIRMED || this.status == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Onaylanmış veya kargodaki sipariş iptal edilemez. Mevcut: " + this.status);
        }
        this.status = OrderStatus.CANCELLED;
    }
}

