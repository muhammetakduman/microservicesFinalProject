package com.microservices.mail_service.listener;

import com.microservices.mail_service.dto.EmailNotificationEvent;
import com.microservices.mail_service.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * notification.queue'yu dinler.
 * Herhangi bir servis (order, payment, auth) bu queue'ya event gönderebilir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MailEventListener {

    private final MailService mailService;

    @RabbitListener(queues = "${rabbitmq.queue.notification:notification.queue}")
    public void handleEmailNotification(EmailNotificationEvent event) {
        log.info("EmailNotificationEvent alındı → to: {}, type: {}",
                event.getTo(), event.getMailType());
        mailService.sendEmail(event);
    }
}

