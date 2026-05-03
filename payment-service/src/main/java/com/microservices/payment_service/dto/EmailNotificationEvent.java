package com.microservices.payment_service.dto;

import lombok.*;

/**
 * mail-service'e notification.queue üzerinden gönderilen mail isteği.
 * Routing key: notification.email
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationEvent {
    /** Alıcı e-posta adresi */
    private String to;
    /** Mail konusu */
    private String subject;
    /** Mail içeriği (HTML) */
    private String body;
    /** PAYMENT_SUCCESS, PAYMENT_FAILED, ORDER_CONFIRMED, vb. */
    private String mailType;
    /** İlgili orderId veya başka entity ID */
    private Long referenceId;
}

