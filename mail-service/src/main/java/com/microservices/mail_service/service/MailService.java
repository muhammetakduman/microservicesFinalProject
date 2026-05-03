package com.microservices.mail_service.service;

import com.microservices.mail_service.dto.EmailNotificationEvent;
import com.microservices.mail_service.entity.MailLog;
import com.microservices.mail_service.entity.MailStatus;
import com.microservices.mail_service.repository.MailLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Gmail SMTP üzerinden mail gönderim servisi.
 *
 * Kimlik bilgileri config-server'daki mail-service-local.properties'ten gelir.
 * (Gitignore ile korunur — prod'da environment variable kullanılmalı)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final MailLogRepository mailLogRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${mail.from.name:Marketplace}")
    private String fromName;

    /**
     * Mail gönderir ve sonucu DB'ye loglar.
     */
    public void sendEmail(EmailNotificationEvent event) {
        log.info("Mail gönderiliyor → {}, konu: {}", event.getTo(), event.getSubject());

        MailLog log_ = MailLog.builder()
                .toEmail(event.getTo())
                .subject(event.getSubject())
                .body(event.getBody())
                .mailType(event.getMailType())
                .referenceId(event.getReferenceId())
                .status(MailStatus.PENDING)
                .build();
        mailLogRepository.save(log_);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(event.getTo());
            helper.setSubject(event.getSubject());
            // HTML içerik desteği
            helper.setText(event.getBody(), true);

            mailSender.send(message);

            log_.setStatus(MailStatus.SENT);
            mailLogRepository.save(log_);
            log.info("Mail gönderildi ✓ → {}", event.getTo());

        } catch (Exception e) {
            log_.setStatus(MailStatus.FAILED);
            log_.setErrorMessage(e.getMessage());
            mailLogRepository.save(log_);
            log.error("Mail gönderilemedi → {}, hata: {}", event.getTo(), e.getMessage());
        }
    }

    /** Son 50 mail logu döner */
    public List<MailLog> getLogs() {
        return mailLogRepository.findTop50ByOrderByCreatedAtDesc();
    }

    /** Başarısız mailleri döner */
    public List<MailLog> getFailedLogs() {
        return mailLogRepository.findByStatus(MailStatus.FAILED);
    }
}

