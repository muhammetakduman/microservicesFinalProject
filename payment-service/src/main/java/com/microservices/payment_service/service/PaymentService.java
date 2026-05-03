package com.microservices.payment_service.service;

import com.microservices.payment_service.dto.*;
import com.microservices.payment_service.entity.PaymentStatus;
import com.microservices.payment_service.repository.PaymentRepository;
import com.microservices.payment_service.service.IyzicoHttpClient.IyzicoPaymentResult;
import com.microservices.payment_service.service.IyzicoHttpClient.IyzicoRefundResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * iyzico ödeme servisi — SDK kullanılmıyor.
 * Ödeme sonucunda order-service'e SAGA eventi + mail-service'e bildirim gönderilir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final IyzicoHttpClient iyzicoHttpClient;

    @Value("${rabbitmq.exchange:marketplace.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.payment-success:payment.success}")
    private String paymentSuccessRoutingKey;

    @Value("${rabbitmq.routing-key.payment-failed:payment.failed}")
    private String paymentFailedRoutingKey;

    @Value("${rabbitmq.routing-key.notification-email:notification.email}")
    private String notificationEmailRoutingKey;

    // ──────────────────────────────────────────────
    // ÖDEME İŞLEMİ
    // ──────────────────────────────────────────────

    @Transactional
    public PaymentResponse processPayment(Long orderId, Long customerId, BigDecimal amount,
                                          String cardNumber, String cardHolder,
                                          String expireMonth, String expireYear, String cvc,
                                          String customerEmail) {

        log.info("Ödeme başlatılıyor — orderId: {}, amount: {}, email: {}", orderId, amount, customerEmail);

        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            log.warn("Zaten ödeme kaydı var — orderId: {}", orderId);
            return toResponse(paymentRepository.findByOrderId(orderId).get());
        }

        com.microservices.payment_service.entity.Payment entity =
                com.microservices.payment_service.entity.Payment.builder()
                        .orderId(orderId).customerId(customerId).amount(amount)
                        .status(PaymentStatus.PENDING).conversationId(orderId.toString())
                        .build();
        paymentRepository.save(entity);

        try {
            IyzicoPaymentResult result = iyzicoHttpClient.createPayment(
                    orderId, customerId, amount, cardNumber, cardHolder, expireMonth, expireYear, cvc);

            if (result.success()) {
                entity.setStatus(PaymentStatus.SUCCESS);
                entity.setIyzicoPaymentId(result.paymentId());
                entity.setIyzicoPaymentTransactionId(result.transactionId());
                paymentRepository.save(entity);
                log.info("Ödeme BAŞARILI — orderId: {}, paymentId: {}", orderId, result.paymentId());

                rabbitTemplate.convertAndSend(exchange, paymentSuccessRoutingKey,
                        PaymentSuccessEvent.builder()
                                .orderId(orderId).customerId(customerId)
                                .amount(amount).transactionId(result.paymentId())
                                .customerEmail(customerEmail)
                                .build());

                sendPaymentSuccessMail(customerEmail, orderId, amount);

            } else {
                entity.setStatus(PaymentStatus.FAILED);
                entity.setErrorMessage(result.errorMessage());
                paymentRepository.save(entity);
                log.warn("Ödeme BAŞARISIZ — orderId: {}, hata: {}", orderId, result.errorMessage());

                rabbitTemplate.convertAndSend(exchange, paymentFailedRoutingKey,
                        PaymentFailedEvent.builder()
                                .orderId(orderId).customerId(customerId)
                                .reason(result.errorMessage()).customerEmail(customerEmail)
                                .build());

                sendPaymentFailedMail(customerEmail, orderId, result.errorMessage());
            }

        } catch (Exception ex) {
            log.error("iyzico hatası — orderId: {}", orderId, ex);
            entity.setStatus(PaymentStatus.FAILED);
            entity.setErrorMessage("Sunucu hatası: " + ex.getMessage());
            paymentRepository.save(entity);

            rabbitTemplate.convertAndSend(exchange, paymentFailedRoutingKey,
                    PaymentFailedEvent.builder()
                            .orderId(orderId).customerId(customerId)
                            .reason("Sunucu hatası").customerEmail(customerEmail)
                            .build());

            sendPaymentFailedMail(customerEmail, orderId, "Teknik bir hata oluştu, lütfen tekrar deneyin.");
        }

        return toResponse(entity);
    }

    // ──────────────────────────────────────────────
    // SORGULAR
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(Long orderId) {
        return toResponse(paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme bulunamadı — orderId: " + orderId)));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long paymentId) {
        return toResponse(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme bulunamadı — id: " + paymentId)));
    }

    // ──────────────────────────────────────────────
    // İADE
    // ──────────────────────────────────────────────

    @Transactional
    public PaymentResponse refund(Long paymentId, BigDecimal refundAmount) {
        com.microservices.payment_service.entity.Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme bulunamadı — id: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESS)
            throw new IllegalStateException("Yalnızca başarılı ödemeler iade edilebilir.");

        BigDecimal amount = refundAmount != null ? refundAmount : payment.getAmount();
        IyzicoRefundResult result = iyzicoHttpClient.refundPayment(
                payment.getIyzicoPaymentTransactionId(), amount, payment.getConversationId());

        if (result.success()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundTransactionId(result.transactionId());
            paymentRepository.save(payment);
            log.info("İade BAŞARILI — paymentId: {}", paymentId);
        } else {
            throw new RuntimeException("İade reddedildi: " + result.errorMessage());
        }

        return toResponse(payment);
    }

    // ──────────────────────────────────────────────
    // MAİL YARDIMCI METODLARI
    // ──────────────────────────────────────────────

    private void sendPaymentSuccessMail(String customerEmail, Long orderId, BigDecimal amount) {
        if (customerEmail == null || customerEmail.isBlank()) {
            log.warn("customerEmail boş — başarı maili gönderilemedi, orderId: {}", orderId);
            return;
        }
        String subject = "✅ Ödemeniz Başarıyla Alındı — Sipariş #" + orderId;
        String body = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px'>"
                + "<h2 style='color:#27ae60'>Ödemeniz Başarıyla Tamamlandı!</h2>"
                + "<p>Merhaba,</p>"
                + "<p><strong>#" + orderId + "</strong> numaralı siparişiniz için ödeme başarıyla alınmıştır.</p>"
                + "<table style='border-collapse:collapse;width:100%'>"
                + "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Sipariş No</strong></td>"
                + "<td style='padding:8px;border:1px solid #ddd'>#" + orderId + "</td></tr>"
                + "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Tutar</strong></td>"
                + "<td style='padding:8px;border:1px solid #ddd'>" + amount + " TL</td></tr>"
                + "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Durum</strong></td>"
                + "<td style='padding:8px;border:1px solid #ddd;color:#27ae60'><strong>BAŞARILI ✅</strong></td></tr>"
                + "</table>"
                + "<p style='margin-top:20px'>Siparişiniz hazırlanmaya başlanmıştır. 🎉</p>"
                + "<hr/><p style='color:#999;font-size:12px'>Bu mail otomatik gönderilmiştir.</p>"
                + "</div>";
        sendNotificationMail(customerEmail, subject, body, "PAYMENT_SUCCESS", orderId);
    }

    private void sendPaymentFailedMail(String customerEmail, Long orderId, String reason) {
        if (customerEmail == null || customerEmail.isBlank()) {
            log.warn("customerEmail boş — başarısızlık maili gönderilemedi, orderId: {}", orderId);
            return;
        }
        String subject = "❌ Ödemeniz Gerçekleşemedi — Sipariş #" + orderId;
        String body = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px'>"
                + "<h2 style='color:#e74c3c'>Ödemeniz Tamamlanamadı</h2>"
                + "<p>Merhaba,</p>"
                + "<p><strong>#" + orderId + "</strong> numaralı siparişiniz için ödeme işlemi gerçekleşemedi.</p>"
                + "<table style='border-collapse:collapse;width:100%'>"
                + "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Sipariş No</strong></td>"
                + "<td style='padding:8px;border:1px solid #ddd'>#" + orderId + "</td></tr>"
                + "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Hata Sebebi</strong></td>"
                + "<td style='padding:8px;border:1px solid #ddd;color:#e74c3c'>"
                + (reason != null ? reason : "Bilinmiyor") + "</td></tr>"
                + "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Durum</strong></td>"
                + "<td style='padding:8px;border:1px solid #ddd;color:#e74c3c'><strong>BAŞARISIZ ❌</strong></td></tr>"
                + "</table>"
                + "<p style='margin-top:20px'>Lütfen kart bilgilerinizi kontrol ederek tekrar deneyiniz.</p>"
                + "<hr/><p style='color:#999;font-size:12px'>Bu mail otomatik gönderilmiştir.</p>"
                + "</div>";
        sendNotificationMail(customerEmail, subject, body, "PAYMENT_FAILED", orderId);
    }

    private void sendNotificationMail(String to, String subject, String body, String mailType, Long referenceId) {
        try {
            EmailNotificationEvent event = EmailNotificationEvent.builder()
                    .to(to).subject(subject).body(body)
                    .mailType(mailType).referenceId(referenceId)
                    .build();
            rabbitTemplate.convertAndSend(exchange, notificationEmailRoutingKey, event);
            log.info("Mail notification gönderildi → to: {}, type: {}", to, mailType);
        } catch (Exception ex) {
            log.error("Mail notification gönderilemedi → to: {}, type: {}", to, mailType, ex);
        }
    }

    // ──────────────────────────────────────────────
    // DÖNÜŞÜM
    // ──────────────────────────────────────────────

    private PaymentResponse toResponse(com.microservices.payment_service.entity.Payment p) {
        return PaymentResponse.builder()
                .id(p.getId()).orderId(p.getOrderId()).customerId(p.getCustomerId())
                .amount(p.getAmount()).status(p.getStatus()).iyzicoPaymentId(p.getIyzicoPaymentId())
                .errorMessage(p.getErrorMessage()).createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }
}

