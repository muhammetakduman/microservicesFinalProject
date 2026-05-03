package com.microservices.mail_service.dto;

import lombok.*;

/**
 * RabbitMQ'dan gelen veya REST'ten gönderilen mail isteği.
 * Queue: notification.queue  |  Routing key: notification.email
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

    /** Mail içeriği (HTML veya düz metin) */
    private String body;

    /** ORDER_CONFIRMED, PAYMENT_FAILED, REGISTER, PASSWORD_RESET, vb. */
    private String mailType;

    /** İlgili entity ID (orderId, userId, vs.) */
    private Long referenceId;
}

