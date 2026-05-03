package com.microservices.shopping_cart_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Müşterinin alışveriş sepeti.
 *
 * Tasarım kararları:
 *  - Her müşterinin TEK bir sepeti vardır (customerId unique constraint).
 *  - Sepet kalemleri ayrı CartItem entity'sinde tutulur (SRP).
 *  - totalAmount @Transient olarak hesaplanır — DB'de tutulmaz, her seferinde kalemlerden üretilir.
 *  - Cross-service JPA yasak: customerId sadece bir Long referans (user-service'e JPA ilişkisi yok).
 */
@Entity
@Table(name = "shopping_carts",
        uniqueConstraints = @UniqueConstraint(columnNames = "customer_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Sepetin sahibi müşteri.
     * user-service'deki User'a ID referansı — JPA @ManyToOne değil.
     */
    @Column(name = "customer_id", nullable = false, unique = true)
    private Long customerId;

    /**
     * Sepetteki ürün kalemleri.
     * cascade = ALL → sepet silinince kalemler de silinir (orphanRemoval).
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    /** Sepet oluşturulma zamanı */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Son güncelleme zamanı */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // -------------------------------------------------------
    // Rich Domain Model — iş mantığı entity içinde
    // -------------------------------------------------------

    /**
     * Sepete kalem ekler ya da mevcutsa miktarı artırır.
     * Tekrar eden ürün kontrolü service yerine domain nesnesinde yapılır (Tell, Don't Ask).
     *
     * @param newItem Eklenecek kalem
     */
    public void addItem(CartItem newItem) {
        // Aynı ürün sepette varsa sadece miktarını artır
        items.stream()
                .filter(i -> i.getProductId().equals(newItem.getProductId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.increaseQuantity(newItem.getQuantity()),
                        () -> {
                            newItem.setCart(this);
                            items.add(newItem);
                        }
                );
    }

    /**
     * Sepetten belirli bir ürün kalemini kaldırır.
     * @param productId Silinecek ürünün ID'si
     */
    public void removeItem(Long productId) {
        items.removeIf(i -> i.getProductId().equals(productId));
    }

    /**
     * Sepeti tamamen boşaltır.
     * Sipariş onaylandığında (PaymentSuccess) çağrılır.
     */
    public void clear() {
        items.clear();
    }

    /**
     * Sepetin toplam tutarını hesaplar.
     * DB'de tutulmaz; her seferinde kalemlerden dinamik olarak üretilir.
     *
     * @return Toplam tutar
     */
    @Transient
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Sepet boş mu?
     */
    @Transient
    public boolean isEmpty() {
        return items.isEmpty();
    }
}

