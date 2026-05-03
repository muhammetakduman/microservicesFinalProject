package com.microservices.mail_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Gönderilen / gönderilemeyen mail log kaydı.
 */
@Entity
@Table(name = "mail_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String toEmail;

    @Column(nullable = false)
    private String subject;

    @Column(length = 2000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MailStatus status = MailStatus.PENDING;

    /** Başarısız gönderimde hata mesajı */
    @Column(length = 500)
    private String errorMessage;

    /** İlgili sipariş / kullanıcı referansı (opsiyonel) */
    @Column
    private Long referenceId;

    /** Mail tipi: ORDER_CONFIRMED, PAYMENT_FAILED, REGISTER, vs. */
    @Column
    private String mailType;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

