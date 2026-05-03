package com.microservices.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ödeme kaydı.
 * Her sipariş için tek bir Payment satırı oluşturulur.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** order-service'deki Order ID (JPA ilişkisi yok — microservices kuralı) */
    @Column(nullable = false, unique = true)
    private Long orderId;

    /** Müşteri ID (user-service referans) */
    @Column(nullable = false)
    private Long customerId;

    /** Ödenen tutar */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /** iyzico conversationId — orderId ile aynı gönderilir */
    @Column
    private String conversationId;

    /** iyzico'dan dönen payment ID */
    @Column
    private String iyzicoPaymentId;

    /** iyzico transaction ID (her kalem için ayrı) */
    @Column
    private String iyzicoPaymentTransactionId;

    /** Başarısız işlem hata mesajı */
    @Column(length = 500)
    private String errorMessage;

    /** İade işlem ID */
    @Column
    private String refundTransactionId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

