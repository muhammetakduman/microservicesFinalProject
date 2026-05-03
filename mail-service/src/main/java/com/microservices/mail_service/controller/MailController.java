package com.microservices.mail_service.controller;

import com.microservices.mail_service.dto.EmailNotificationEvent;
import com.microservices.mail_service.entity.MailLog;
import com.microservices.mail_service.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Mail REST API.
 *
 * POST /api/notifications/email    → Direkt mail gönder (test/admin)
 * GET  /api/notifications/logs     → Son 50 mail logu
 * GET  /api/notifications/failed   → Başarısız mailler
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class MailController {

    private final MailService mailService;

    /**
     * GET /api/notifications/test?to=hedef@email.com
     * SMTP sunucusunu test etmek için hızlı mail gönderir.
     * to parametresi verilmezse akdumanmuhammet34@gmail.com'a gönderilir.
     */
    @GetMapping("/test")
    public ResponseEntity<String> sendTestMail(
            @RequestParam(defaultValue = "akdumanmuhammet34@gmail.com") String to) {
        log.info("SMTP test maili gönderiliyor → {}", to);
        EmailNotificationEvent event = EmailNotificationEvent.builder()
                .to(to)
                .subject("✅ SMTP Test — Marketplace Mail Servisi Çalışıyor")
                .body("<div style='font-family:Arial,sans-serif;padding:20px'>"
                        + "<h2 style='color:#27ae60'>🎉 SMTP Bağlantısı Başarılı!</h2>"
                        + "<p>Bu mail, mail-service SMTP yapılandırmasını doğrulamak için gönderilmiştir.</p>"
                        + "<ul>"
                        + "<li>Gönderen: <strong>akdumanmxc@gmail.com</strong></li>"
                        + "<li>Servis: <strong>mail-service</strong></li>"
                        + "<li>Zaman: <strong>" + java.time.LocalDateTime.now() + "</strong></li>"
                        + "</ul>"
                        + "<hr/><p style='color:#999;font-size:12px'>Marketplace Microservices — otomatik test maili</p>"
                        + "</div>")
                .mailType("SMTP_TEST")
                .referenceId(0L)
                .build();
        mailService.sendEmail(event);
        return ResponseEntity.ok("Test maili gönderildi → " + to);
    }

    /**
     * POST /api/notifications/email
     * Direkt mail gönderir (admin paneli veya test için).
     */
    @PostMapping("/email")
    public ResponseEntity<String> sendEmail(@RequestBody EmailNotificationEvent request) {
        log.info("POST /api/notifications/email → {}", request.getTo());
        mailService.sendEmail(request);
        return ResponseEntity.ok("Mail kuyruğa alındı: " + request.getTo());
    }

    /**
     * GET /api/notifications/logs
     * Son 50 mail logu döner.
     */
    @GetMapping("/logs")
    public ResponseEntity<List<MailLog>> getLogs() {
        return ResponseEntity.ok(mailService.getLogs());
    }

    /**
     * GET /api/notifications/failed
     * Başarısız gönderileri döner.
     */
    @GetMapping("/failed")
    public ResponseEntity<List<MailLog>> getFailedLogs() {
        return ResponseEntity.ok(mailService.getFailedLogs());
    }
}

